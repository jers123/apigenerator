package org.jers.apigenerator.core.project;

import org.jers.apigenerator.core.architecture.ArchitectureSpec;
import org.jers.apigenerator.core.architecture.ArchitectureStyle;
import org.jers.apigenerator.core.architecture.ModuleLayout;
import  org.jers.apigenerator.core.database.DatabaseEngine;
import org.jers.apigenerator.core.selection.BuildTool;

import java.util.Map;
import java.util.Set;

/** Objetos válidos de proyecto para los tests del paquete. */
final class ProjectFixtures {

    /** Motor con servidor, con la misma plantilla y grupo de driver que el de fábrica. */
    static final DatabaseEngine POSTGRESQL = new DatabaseEngine("postgresql", "PostgreSQL", "postgresql-driver",
            false, 5432, "postgres", "jdbc:postgresql://{host}:{port}/{database}");

    /** Motor embebido, con la misma plantilla y grupo de driver que el de fábrica. */
    static final DatabaseEngine H2 = new DatabaseEngine("h2", "H2", "h2-database",
            true, null, "sa", "jdbc:h2:mem:{database};DB_CLOSE_DELAY=-1");

    private ProjectFixtures() {
    }

    static ProjectMetadata metadata() {
        return new ProjectMetadata("org.jers", "api-clientes", "API Clientes", "Gestión de clientes");
    }

    static BuildSettings build(final boolean reactive) {
        return new BuildSettings(BuildTool.MAVEN, 25, Packaging.JAR, reactive, 8080);
    }

    static ArchitectureSpec architecture() {
        return new ArchitectureSpec(ArchitectureStyle.LAYERED, ModuleLayout.SINGLE_MODULE, Set.of());
    }

    static GenerationOptions options() {
        return new GenerationOptions(true, true, ConfigFormat.YAML);
    }

    static DatabaseConnection postgres() {
        return new DatabaseConnection(POSTGRESQL, "localhost", 5432, "clientes", "postgres", "secreto");
    }

    static DatabaseConnection h2() {
        return new DatabaseConnection(H2, null, 0, "testdb", "sa", null);
    }

    static ProjectSpec spec(final DatabaseConnection database, final DatabaseConnection testDatabase) {
        return new ProjectSpec(metadata(), build(false), Set.of("spring-web"), database, testDatabase, Map.of(),
                architecture(), options());
    }
}