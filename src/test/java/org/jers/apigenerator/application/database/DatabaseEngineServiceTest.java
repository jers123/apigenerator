package org.jers.apigenerator.application.database;

import org.jers.apigenerator.core.catalog.*;
import org.jers.apigenerator.core.database.DatabaseEngine;
import org.jers.apigenerator.core.database.DatabaseEngineDefinition;
import org.jers.apigenerator.core.database.DatabaseEngineRepository;
import org.jers.apigenerator.testsupport.InMemoryDatabaseEngineRepository;
import org.jers.apigenerator.testsupport.InMemoryDependencyCatalogRepository;
import org.jers.apigenerator.testsupport.TestCatalogs;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class DatabaseEngineServiceTest {

    /** Repositorio en memoria que puede fallar al escribir, como un disco lleno. */
    static final class FailingRepository implements DatabaseEngineRepository {
        private final InMemoryDatabaseEngineRepository delegate =
                new InMemoryDatabaseEngineRepository(InMemoryDatabaseEngineRepository.postgresql());
        private boolean failWrites;

        @Override
        public List<DatabaseEngine> findAll() {
            return delegate.findAll();
        }

        @Override
        public Optional<DatabaseEngine> findById(final String id) {
            return delegate.findById(id);
        }

        @Override
        public DatabaseEngine create(final DatabaseEngine engine) {
            failIfRequested();
            return delegate.create(engine);
        }

        @Override
        public DatabaseEngine update(final DatabaseEngine engine) {
            failIfRequested();
            return delegate.update(engine);
        }

        private void failIfRequested() {
            if (failWrites) {
                throw new IllegalStateException("disco lleno");
            }
        }
    }

    private final FailingRepository engines = new FailingRepository();
    private final InMemoryDependencyCatalogRepository catalog =
            new InMemoryDependencyCatalogRepository(TestCatalogs.small());
    private final DatabaseEngineService service = new DatabaseEngineService(engines, catalog);

    private static DatabaseEngineDefinition definition(final String engineId, final String groupId,
                                                       final String groupName) {
        DependencyGroup group = new DependencyGroup(groupId, groupName, DependencyCategory.DATABASE,
                List.of(new Dependency(groupName + " JDBC", "org.example", groupId, null, DependencyKind.LIBRARY,
                        MavenScope.COMPILE, GradleScope.IMPLEMENTATION, true, false)), false);
        return new DatabaseEngineDefinition(new DatabaseEngine(engineId, engineId, groupId, false, 3306, "root",
                "jdbc:" + engineId + "://{host}:{port}/{database}"), group);
    }

    @Test
    void createSavesTheEngineAndItsGroup() {
        service.create(definition("mariadb", "mariadb-driver", "MariaDB Driver"));

        assertThat(engines.findById("mariadb")).isPresent();
        assertThat(catalog.catalog().findGroup("mariadb-driver")).isPresent();
    }

    @Test
    void aFailedCreateRemovesTheGroupAgain() {
        engines.failWrites = true;

        assertThatIllegalStateException()
                .isThrownBy(() -> service.create(definition("mariadb", "mariadb-driver", "MariaDB Driver")));

        assertThat(catalog.catalog()).isEqualTo(TestCatalogs.small());
    }

    @Test
    void aFailedUpdateRestoresThePreviousGroup() {
        engines.failWrites = true;

        assertThatIllegalStateException()
                .isThrownBy(() -> service.update("postgresql",
                        definition("postgresql", "postgresql", "PostgreSQL Renombrado")));

        assertThat(catalog.catalog()).isEqualTo(TestCatalogs.small());
    }

    @Test
    void aFailedUpdateWithANewGroupRemovesIt() {
        engines.failWrites = true;

        assertThatIllegalStateException()
                .isThrownBy(() -> service.update("postgresql",
                        definition("postgresql", "postgresql-driver", "PostgreSQL Nuevo")));

        assertThat(catalog.catalog()).isEqualTo(TestCatalogs.small());
    }
}