package org.jers.apigenerator.infrastructure.security;

import org.jers.apigenerator.core.catalog.Dependency;
import org.jers.apigenerator.core.security.SecurityCatalog;
import org.jers.apigenerator.core.security.SecurityMechanism;
import org.jers.apigenerator.core.security.SecurityRole;
import org.jers.apigenerator.infrastructure.storage.JsonFileStoreFactory;
import org.jers.apigenerator.testsupport.TestSecurityCatalogs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JsonSecurityCatalogRepositoryTest {

    @TempDir
    private Path dataDir;

    private JsonSecurityCatalogRepository repository() {
        return new JsonSecurityCatalogRepository(new JsonFileStoreFactory(JsonMapper.builder().build(), dataDir),
                new ClassPathResource("defaults/security.json"));
    }

    @Test
    void firstRunCopiesTheFactoryMechanisms() {
        SecurityCatalog catalog = repository().catalog();

        assertThat(dataDir.resolve(JsonSecurityCatalogRepository.FILE_NAME)).exists();
        assertThat(catalog.mechanisms()).extracting(SecurityMechanism::id).containsExactly(
                "http-basic", "api-key", "jwt", "oauth2-resource-server", "form-login", "oidc-login",
                "method-security", "cors", "authorization-server");
    }

    @Test
    void factoryMechanismsUseTheSpringBoot4StarterNames() {
        assertThat(repository().catalog().mechanisms())
                .flatExtracting(SecurityMechanism::dependencies)
                .extracting(Dependency::artifactId)
                .noneMatch(artifactId -> artifactId.startsWith("spring-boot-starter-oauth2-"));
    }

    @Test
    void factoryMechanismsLeaveTheirVersionToTheParent() {
        assertThat(repository().catalog().mechanisms())
                .flatExtracting(SecurityMechanism::dependencies)
                .allSatisfy(dependency -> assertThat(dependency.followsSpringBootVersion())
                        .as("%s", dependency.coordinates()).isTrue());
    }

    @Test
    void theAuthorizationServerIsTheOnlyOneThatIsNotReactive() {
        assertThat(repository().catalog().mechanisms())
                .filteredOn(mechanism -> !mechanism.reactiveSupported())
                .extracting(SecurityMechanism::role)
                .containsExactly(SecurityRole.AUTHORIZATION_SERVER);
    }

    @Test
    void theFactoryCatalogSurvivesAWriteAndReadRoundTrip() {
        SecurityCatalog original = repository().catalog();

        repository().modify(catalog -> catalog);

        assertThat(repository().catalog()).isEqualTo(original);
    }

    @Test
    void aSavedChangeSurvivesARestart() {
        repository().modify(catalog -> TestSecurityCatalogs.small());

        assertThat(repository().catalog()).isEqualTo(TestSecurityCatalogs.small());
    }
}