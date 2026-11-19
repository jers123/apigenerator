package org.jers.apigenerator.core.database;

import org.jers.apigenerator.core.catalog.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DatabaseEngineDefinitionTest {

    private static DatabaseEngine engine(final String driverGroupId) {
        return new DatabaseEngine("mariadb", "MariaDB", driverGroupId, false, 3306, "root",
                "jdbc:mariadb://{host}:{port}/{database}");
    }

    private static DependencyGroup group(final String id, final DependencyCategory category) {
        return new DependencyGroup(id, "MariaDB Driver", category, List.of(new Dependency("MariaDB JDBC",
                "org.mariadb.jdbc", "mariadb-java-client", null, DependencyKind.LIBRARY,
                MavenScope.COMPILE, GradleScope.IMPLEMENTATION, true, false)), false);
    }

    @Test
    void acceptsADatabaseGroupWithTheSameIdAsTheEngineDriver() {
        DatabaseEngineDefinition definition = new DatabaseEngineDefinition(
                engine("mariadb-driver"), group("mariadb-driver", DependencyCategory.DATABASE));

        assertThat(definition.driverGroup().id()).isEqualTo(definition.engine().driverGroupId());
    }

    @Test
    void rejectsAGroupThatIsNotOfDatabaseCategory() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DatabaseEngineDefinition(
                        engine("mariadb-driver"), group("mariadb-driver", DependencyCategory.OTHER)))
                .withMessageContaining("DATABASE");
    }

    @Test
    void rejectsAGroupWhoseIdDoesNotMatchTheEngine() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DatabaseEngineDefinition(
                        engine("mysql-driver"), group("mariadb-driver", DependencyCategory.DATABASE)))
                .withMessageContaining("mysql-driver")
                .withMessageContaining("mariadb-driver");
    }
}