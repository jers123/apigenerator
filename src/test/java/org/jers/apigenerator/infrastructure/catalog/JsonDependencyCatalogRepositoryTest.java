package org.jers.apigenerator.infrastructure.catalog;

import org.jers.apigenerator.core.catalog.DependencyCatalog;
import org.jers.apigenerator.core.catalog.DependencyCategory;
import org.jers.apigenerator.core.catalog.DependencyGroup;
import org.jers.apigenerator.core.catalog.DependencyKind;
import org.jers.apigenerator.core.common.DomainValidationException;
import org.jers.apigenerator.infrastructure.storage.JsonFileStoreFactory;
import org.jers.apigenerator.testsupport.TestCatalogs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class JsonDependencyCatalogRepositoryTest {

    private static final Resource DEFAULTS = new ClassPathResource("catalog/dependency-catalog.json");

    @TempDir
    private Path dataDir;

    private JsonDependencyCatalogRepository repository(final Resource defaults) {
        return new JsonDependencyCatalogRepository(
                new JsonFileStoreFactory(JsonMapper.builder().build(), dataDir), defaults);
    }

    private JsonDependencyCatalogRepository repository() {
        return repository(DEFAULTS);
    }

    @Test
    void firstRunCopiesTheFactoryCatalogToTheDataDir() {
        DependencyCatalog catalog = repository().catalog();

        assertThat(dataDir.resolve(JsonDependencyCatalogRepository.FILE_NAME)).exists();
        assertThat(catalog.findGroup("spring-web")).isPresent();
        assertThat(catalog.groupsIn(DependencyCategory.DATABASE))
                .extracting(DependencyGroup::id)
                .contains("h2-database", "postgresql-driver");
    }

    @Test
    void springBootArtifactsLeaveTheirVersionToTheParent() {
        assertThat(repository().catalog().groups())
                .flatExtracting(DependencyGroup::dependencies)
                .filteredOn(d -> d.groupId().equals("org.springframework.boot"))
                .filteredOn(d -> d.kind() != DependencyKind.PARENT)
                .allSatisfy(d -> assertThat(d.hasExplicitVersion())
                        .as("%s no debe fijar versión: la gestiona el parent", d.coordinates())
                        .isFalse());
    }

    @Test
    void savedCatalogIsServedAtOnceAndSurvivesARestart() {
        JsonDependencyCatalogRepository repository = repository();

        repository.save(TestCatalogs.small());

        assertThat(repository.catalog()).isEqualTo(TestCatalogs.small());
        assertThat(repository().catalog()).isEqualTo(TestCatalogs.small());
    }

    @Test
    void theRealCatalogSurvivesAWriteAndReadRoundTrip() {
        DependencyCatalog original = repository().catalog();

        repository().save(original);

        assertThat(repository().catalog()).isEqualTo(original);
    }

    @Test
    void anInvalidSavedFileStopsStartupNamingTheFileAndTheCause() throws IOException {
        Files.writeString(dataDir.resolve(JsonDependencyCatalogRepository.FILE_NAME), """
                {"groups": [{"id": "roto", "name": "Roto", "category": "OTHER", "dependencies": [
                  {"name": "Sin scopes", "groupId": "g", "artifactId": "a",
                   "kind": "LIBRARY", "includedByDefault": true}]}]}
                """);

        assertThatIllegalStateException()
                .isThrownBy(this::repository)
                .withMessageContaining(JsonDependencyCatalogRepository.FILE_NAME)
                .withMessageContaining("mavenScope");
    }

    @Test
    void missingFactoryCatalogStopsStartupWithClearMessage() {
        assertThatIllegalStateException()
                .isThrownBy(() -> repository(new ClassPathResource("catalog/no-existe.json")))
                .withMessageContaining("no-existe.json");
    }

    @Test
    void modifyAppliesTheChangeAndPersistsIt() {
        JsonDependencyCatalogRepository repository = repository();
        repository.save(TestCatalogs.small());

        DependencyCatalog result = repository.modify(
                catalog -> new DependencyCatalog(catalog.groups().subList(0, 2)));

        assertThat(result.groups()).extracting(DependencyGroup::id).containsExactly("maven", "gradle");
        assertThat(repository().catalog()).isEqualTo(result);
    }

    @Test
    void aFailingChangeLeavesTheCatalogAndTheFileUntouched() throws IOException {
        JsonDependencyCatalogRepository repository = repository();
        Path file = dataDir.resolve(JsonDependencyCatalogRepository.FILE_NAME);
        String before = Files.readString(file);
        DependencyCatalog current = repository.catalog();

        assertThatIllegalArgumentException().isThrownBy(() -> repository.modify(catalog -> {
            throw new DomainValidationException("rechazado");
        }));

        assertThat(repository.catalog()).isSameAs(current);
        assertThat(Files.readString(file)).isEqualTo(before);
    }
}