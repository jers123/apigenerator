package org.jers.apigenerator.infrastructure.security;

import org.jers.apigenerator.core.security.IdentityProvider;
import org.jers.apigenerator.core.security.IdentityProviderCatalog;
import org.jers.apigenerator.infrastructure.storage.JsonFileStoreFactory;
import org.jers.apigenerator.testsupport.TestIdentityProviders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonIdentityProviderRepositoryTest {

    @TempDir
    private Path dataDir;

    private JsonIdentityProviderRepository repository() {
        return new JsonIdentityProviderRepository(new JsonFileStoreFactory(JsonMapper.builder().build(), dataDir),
                new ClassPathResource("defaults/identity-providers.json"));
    }

    private IdentityProvider provider(final String id) {
        return repository().catalog().findProvider(id).orElseThrow();
    }

    @Test
    void firstRunCopiesTheFactoryProviders() {
        assertThat(repository().catalog().providers()).extracting(IdentityProvider::id)
                .containsExactly("keycloak", "auth0", "okta", "entra-id", "cognito", "google", "github");
        assertThat(dataDir.resolve(JsonIdentityProviderRepository.FILE_NAME)).exists();
    }

    @Test
    void googleAndGithubAreOnlyForLogin() {
        assertThat(repository().catalog().providers())
                .filteredOn(provider -> !provider.supportsResourceServer())
                .extracting(IdentityProvider::id)
                .containsExactly("google", "github");
    }

    @Test
    void factoryTemplatesBuildTheDocumentedIssuers() {
        assertThat(provider("entra-id").issuerUri(Map.of("tenantId", "abc")))
                .isEqualTo("https://login.microsoftonline.com/abc/v2.0");
        assertThat(provider("cognito").issuerUri(Map.of("region", "us-east-1", "userPoolId", "us-east-1_abc")))
                .isEqualTo("https://cognito-idp.us-east-1.amazonaws.com/us-east-1_abc");
        assertThat(provider("auth0").issuerUri(Map.of("domain", "t.us.auth0.com"))).isEqualTo("https://t.us.auth0.com/");
        assertThat(provider("okta").issuerUri(Map.of("domain", "org.okta.com")))
                .isEqualTo("https://org.okta.com/oauth2/default");
    }

    @Test
    void theFactoryCatalogSurvivesAWriteAndReadRoundTrip() {
        IdentityProviderCatalog original = repository().catalog();

        repository().modify(catalog -> catalog);

        assertThat(repository().catalog()).isEqualTo(original);
    }

    @Test
    void aSavedChangeSurvivesARestart() {
        repository().modify(catalog -> TestIdentityProviders.small());

        assertThat(repository().catalog()).isEqualTo(TestIdentityProviders.small());
    }
}