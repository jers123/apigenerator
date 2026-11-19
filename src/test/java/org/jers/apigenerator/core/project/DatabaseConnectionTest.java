package org.jers.apigenerator.core.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DatabaseConnectionTest {

    @Test
    void toStringNeverRevealsThePassword() {
        assertThat(ProjectFixtures.postgres().toString())
                .doesNotContain("secreto")
                .contains("password=****");
    }

    @Test
    void serverEngineBuildsItsUrlFromTheConnectionData() {
        assertThat(ProjectFixtures.postgres().jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/clientes");
    }

    @Test
    void serverEngineRequiresHost() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DatabaseConnection(ProjectFixtures.POSTGRESQL, " ", 5432, "app", "postgres", ""))
                .withMessageContaining("host");
    }

    @Test
    void serverEngineRejectsPortOutOfRange() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DatabaseConnection(ProjectFixtures.POSTGRESQL, "localhost", 70_000, "app",
                        "postgres", ""))
                .withMessageContaining("port");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1clientes", "mi-base", "mi base", "base;DROP"})
    void invalidDatabaseNameIsRejected(final String databaseName) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DatabaseConnection(ProjectFixtures.POSTGRESQL, "localhost", 5432, databaseName,
                        "postgres", ""))
                .withMessageContaining("databaseName");
    }

    @Test
    void embeddedEngineDoesNotNeedHostOrPort() {
        assertThat(ProjectFixtures.h2().jdbcUrl()).isEqualTo("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
    }

    @Test
    void missingPasswordBecomesEmpty() {
        assertThat(ProjectFixtures.h2().password()).isEmpty();
    }
}