package org.jers.apigenerator.infrastructure.database;

import org.jers.apigenerator.core.catalog.DependencyCatalog;
import org.jers.apigenerator.core.database.DatabaseEngine;
import org.jers.apigenerator.core.database.DatabaseEngineNotFoundException;
import org.jers.apigenerator.core.database.DatabaseKind;
import org.jers.apigenerator.core.database.DuplicateDatabaseEngineException;
import org.jers.apigenerator.infrastructure.catalog.JsonDependencyCatalogRepository;
import org.jers.apigenerator.infrastructure.storage.JsonFileStoreFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonDatabaseEngineRepositoryTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final ClassPathResource DEFAULTS = new ClassPathResource("defaults/databases.json");

    /** Id que nunca estará en los valores de fábrica. */
    private static final String TEST_ENGINE_ID = "motor-prueba";

    @TempDir
    private Path dataDir;

    private JsonDatabaseEngineRepository repository() {
        return new JsonDatabaseEngineRepository(new JsonFileStoreFactory(MAPPER, dataDir), DEFAULTS);
    }

    /** Motor ficticio, ajeno a los valores de fábrica, para crear y actualizar. */
    private static DatabaseEngine testEngine(final String label) {
        return new DatabaseEngine(TEST_ENGINE_ID, label, "mysql-driver", false, 3307, "root",
                "jdbc:mysql://{host}:{port}/{database}");
    }

    @Test
    void firstRunCopiesTheFactoryDefaults() {
        assertThat(repository().findById("postgresql")).isPresent();
        assertThat(dataDir.resolve("databases.json")).exists();
    }

    @Test
    void theTestEngineIsNotAFactoryEngine() {
        assertThat(repository().findById(TEST_ENGINE_ID)).isEmpty();
    }

    @Test
    void createdEngineSurvivesARestart() {
        repository().create(testEngine("Motor de prueba"));

        assertThat(repository().findById(TEST_ENGINE_ID)).contains(testEngine("Motor de prueba"));
    }

    @Test
    void updatedEngineSurvivesARestart() {
        repository().update(new DatabaseEngine("postgresql", "PostgreSQL 18", "postgresql-driver", false, 5433,
                "postgres", "jdbc:postgresql://{host}:{port}/{database}", DatabaseKind.RELATIONAL, true));

        DatabaseEngine reloaded = repository().findById("postgresql").orElseThrow();
        assertThat(reloaded.label()).isEqualTo("PostgreSQL 18");
        assertThat(reloaded.defaultPort()).isEqualTo(5433);
        assertThat(reloaded.supportsSequences()).isTrue();
    }

    @Test
    void duplicateIdIsRejectedAndFileStaysTheSame() throws IOException {
        JsonDatabaseEngineRepository repository = repository();
        repository.create(testEngine("Motor de prueba"));
        String before = Files.readString(dataDir.resolve("databases.json"));

        assertThatThrownBy(() -> repository.create(testEngine("Otro")))
                .isInstanceOf(DuplicateDatabaseEngineException.class);
        assertThat(Files.readString(dataDir.resolve("databases.json"))).isEqualTo(before);
    }

    @Test
    void updatingAnUnknownEngineFails() {
        assertThatThrownBy(() -> repository().update(testEngine("Motor de prueba")))
                .isInstanceOf(DatabaseEngineNotFoundException.class);
    }

    @Test
    void corruptFileStopsStartupNamingTheFile() throws IOException {
        Files.writeString(dataDir.resolve("databases.json"), "{ esto no es json");

        assertThatIllegalStateException()
                .isThrownBy(this::repository)
                .withMessageContaining("databases.json");
    }

    @Test
    void aFileWithoutKindOrSequencesStillLoadsAsRelationalWithoutSequences() throws IOException {
        Files.writeString(dataDir.resolve("databases.json"), """
                {"engines": [{"id": "postgresql", "label": "PostgreSQL", "driverGroupId": "postgresql-driver",
                  "embedded": false, "defaultPort": 5432, "defaultUsername": "postgres",
                  "jdbcUrlTemplate": "jdbc:postgresql://{host}:{port}/{database}"}]}
                """);

        DatabaseEngine engine = repository().findById("postgresql").orElseThrow();

        assertThat(engine.kind()).isEqualTo(DatabaseKind.RELATIONAL);
        assertThat(engine.supportsSequences()).isFalse();
    }

    static Stream<Arguments> factoryUrls() {
        return Stream.of(
                Arguments.of("ibm-db2", "jdbc:db2://db.local:50000/app"),
                Arguments.of("mariadb", "jdbc:mariadb://db.local:3306/app"),
                Arguments.of("mysql", "jdbc:mysql://db.local:3306/app"),
                Arguments.of("oracle", "jdbc:oracle:thin:@//db.local:1521/app"),
                Arguments.of("postgresql", "jdbc:postgresql://db.local:5432/app"),
                Arguments.of("sql-server",
                        "jdbc:sqlserver://db.local:1433;databaseName=app;encrypt=true;trustServerCertificate=true"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("factoryUrls")
    void factoryTemplatesProduceTheCorrectUrls(final String id, final String expected) {
        DatabaseEngine engine = repository().findById(id).orElseThrow();

        assertThat(engine.jdbcUrl("db.local", engine.defaultPort(), "app")).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} secuencias={1}")
    @CsvSource({"h2,true", "ibm-db2,true", "mariadb,true", "mysql,false", "oracle,true", "postgresql,true",
            "sql-server,true"})
    void factoryEnginesAreRelationalAndDeclareTheirSequences(final String id, final boolean sequences) {
        DatabaseEngine engine = repository().findById(id).orElseThrow();

        assertThat(engine.kind()).isEqualTo(DatabaseKind.RELATIONAL);
        assertThat(engine.supportsSequences()).isEqualTo(sequences);
    }

    @Test
    void everyFactoryEngineHasItsDriverGroupInTheCatalog() {
        DependencyCatalog catalog = new JsonDependencyCatalogRepository(new JsonFileStoreFactory(MAPPER, dataDir),
                new ClassPathResource("catalog/dependency-catalog.json")).catalog();

        assertThat(repository().findAll())
                .allSatisfy(engine -> assertThat(catalog.findGroup(engine.driverGroupId()))
                        .as("grupo '%s' de %s", engine.driverGroupId(), engine.id())
                        .isPresent());
    }
}