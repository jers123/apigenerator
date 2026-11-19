package org.jers.apigenerator.core.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DatabaseEngineTest {

    private static final String MARIADB_URL = "jdbc:mariadb://{host}:{port}/{database}";

    private static DatabaseEngine server(final String template) {
        return new DatabaseEngine("mariadb", "MariaDB", "mariadb-driver", false, 3306, "root", template);
    }

    @Test
    void rendersTheTemplateWithTheConnectionData() {
        assertThat(server(MARIADB_URL).jdbcUrl("db.local", 3307, "app"))
                .isEqualTo("jdbc:mariadb://db.local:3307/app");
    }

    @Test
    void embeddedEngineDoesNotNeedHostPortOrDefaultPort() {
        DatabaseEngine h2 = new DatabaseEngine("h2", "H2", "h2-database", true, 9999, "sa",
                "jdbc:h2:mem:{database}");

        assertThat(h2.defaultPort()).isNull();
        assertThat(h2.jdbcUrl(null, 0, "testdb")).isEqualTo("jdbc:h2:mem:testdb");
    }

    @Test
    void unknownPlaceholderIsRejectedNamingIt() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> server("jdbc:mariadb://{host}:{port}/{db}"))
                .withMessageContaining("{db}");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "mariadb://{host}:{port}/{database}",   // no empieza por jdbc:
            "jdbc:mariadb://{host}:{port}/app",     // sin {database}
            "jdbc:mariadb://localhost/{database}"}) // con servidor pero sin {host}/{port}
    void invalidTemplatesAreRejected(final String template) {
        assertThatIllegalArgumentException().isThrownBy(() -> server(template));
    }

    @Test
    void serverEngineNeedsAValidDefaultPort() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DatabaseEngine("x", "X", "x-driver", false, null, "root",
                        "jdbc:x://{host}:{port}/{database}"))
                .withMessageContaining("defaultPort");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SqlServer", "sql_server", "-sql", "sql-"})
    void invalidIdIsRejected(final String id) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DatabaseEngine(id, "X", "x-driver", false, 1, "u",
                        "jdbc:x://{host}:{port}/{database}"))
                .withMessageContaining("id");
    }

    @Test
    void theShortConstructorCreatesARelationalEngineWithoutSequences() {
        DatabaseEngine engine = server(MARIADB_URL);

        assertThat(engine.kind()).isEqualTo(DatabaseKind.RELATIONAL);
        assertThat(engine.supportsSequences()).isFalse();
    }

    @Test
    void aMissingKindMeansRelational() {
        DatabaseEngine engine = new DatabaseEngine("mariadb", "MariaDB", "mariadb-driver", false, 3306, "root",
                MARIADB_URL, null, true);

        assertThat(engine.kind()).isEqualTo(DatabaseKind.RELATIONAL);
        assertThat(engine.supportsSequences()).isTrue();
    }

    @Test
    void documentEnginesAreNotSupportedYet() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DatabaseEngine("mongodb", "MongoDB", "mongodb-driver", false, 27017, "root",
                        MARIADB_URL, DatabaseKind.DOCUMENT, false))
                .withMessageContaining("Documentos (NoSQL)");
    }
}