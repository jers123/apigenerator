package org.jers.apigenerator.core.project;

import org.jers.apigenerator.core.architecture.ArchitectureSpec;
import org.jers.apigenerator.core.common.DomainValidationException;
import org.jers.apigenerator.core.common.Preconditions;
import org.jers.apigenerator.core.model.DomainModel;
import org.jers.apigenerator.core.security.SecuritySelection;
import org.jers.apigenerator.core.selection.SelectionRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Descripción completa del proyecto que se va a generar.
 *
 * <p>Reglas de bases de datos: la de pruebas solo existe si hay una de
 * desarrollo, y solo si se generan pruebas, porque la usan las de
 * integración. Sin base de desarrollo los datos viven en la RAM del
 * proceso.</p>
 *
 * @param metadata           identidad del proyecto
 * @param build              opciones de construcción y ejecución
 * @param dependencyGroupIds ids de los grupos elegidos por el usuario
 * @param database           base de datos de desarrollo, o {@code null} si
 *                           los datos viven en memoria
 * @param testDatabase       base de datos de pruebas, o {@code null} si las
 *                           pruebas trabajan en memoria
 * @param versionOverrides   versiones elegidas por el usuario, por clave
 *                           {@code groupId:artifactId}; lo que no figura usa
 *                           la versión del catálogo
 * @param architecture       estilo, reparto en módulos y patrones
 * @param options            Javadoc, pruebas y formato de configuración
 * @param security           seguridad elegida; {@link SecuritySelection#none()} si no tiene
 * @param model              modelo de dominio; {@code null} equivale a {@link DomainModel#empty()}
 */
public record ProjectSpec(
        ProjectMetadata metadata,
        BuildSettings build,
        Set<String> dependencyGroupIds,
        DatabaseConnection database,
        DatabaseConnection testDatabase,
        Map<String, String> versionOverrides,
        ArchitectureSpec architecture,
        GenerationOptions options,
        SecuritySelection security,
        DomainModel model) {

    /** Clave de un artefacto: {@code groupId:artifactId}, sin espacios. */
    private static final Pattern COORDINATES = Pattern.compile("[^:\\s]+:[^:\\s]+");

    /**
     * Valida el proyecto y protege sus colecciones.
     *
     * @throws NullPointerException      si los metadatos, el build, la
     *                                   arquitectura, las opciones, la
     *                                   seguridad o alguna colección es {@code null}
     * @throws DomainValidationException si una clave de versión no es válida,
     *                                   una versión está en blanco, o la base
     *                                   de pruebas no tiene base de desarrollo
     *                                   o pruebas que la usen
     */
    public ProjectSpec {
        Objects.requireNonNull(metadata, "metadata es obligatorio");
        Objects.requireNonNull(build, "build es obligatorio");
        Objects.requireNonNull(dependencyGroupIds, "dependencyGroupIds es obligatorio");
        Objects.requireNonNull(versionOverrides, "versionOverrides es obligatorio");
        Objects.requireNonNull(architecture, "architecture es obligatoria");
        Objects.requireNonNull(options, "options es obligatorio");
        Objects.requireNonNull(security, "security es obligatoria");
        if (testDatabase != null && database == null) {
            throw new DomainValidationException(
                    "Solo se puede elegir una base de pruebas si hay una base de desarrollo");
        }
        if (testDatabase != null && !options.unitTests()) {
            throw new DomainValidationException("La base de pruebas la usan las pruebas de integración: "
                    + "active la generación de pruebas o quite la base de pruebas");
        }
        dependencyGroupIds = Set.copyOf(dependencyGroupIds);
        versionOverrides.forEach((coordinates, version) -> {
            if (coordinates == null || !COORDINATES.matcher(coordinates).matches()) {
                throw new DomainValidationException("Clave de versión inválida '" + coordinates
                        + "': use groupId:artifactId");
            }
            Preconditions.requireText(version, "la versión de " + coordinates);
        });
        versionOverrides = Map.copyOf(versionOverrides);
        model = model == null ? DomainModel.empty() : model;
    }

    /**
     * Crea un proyecto con seguridad y sin modelo.
     *
     * @param metadata           identidad del proyecto
     * @param build              opciones de construcción y ejecución
     * @param dependencyGroupIds ids de los grupos elegidos
     * @param database           base de desarrollo, o {@code null}
     * @param testDatabase       base de pruebas, o {@code null}
     * @param versionOverrides   versiones elegidas por clave {@code groupId:artifactId}
     * @param architecture       estilo, reparto en módulos y patrones
     * @param options            Javadoc, pruebas y formato de configuración
     * @param security           seguridad elegida
     */
    public ProjectSpec(final ProjectMetadata metadata, final BuildSettings build, final Set<String> dependencyGroupIds,
                       final DatabaseConnection database, final DatabaseConnection testDatabase,
                       final Map<String, String> versionOverrides, final ArchitectureSpec architecture,
                       final GenerationOptions options, final SecuritySelection security) {
        this(metadata, build, dependencyGroupIds, database, testDatabase, versionOverrides, architecture, options,
                security, DomainModel.empty());
    }

    /**
     * Crea un proyecto sin seguridad ni modelo.
     *
     * @param metadata           identidad del proyecto
     * @param build              opciones de construcción y ejecución
     * @param dependencyGroupIds ids de los grupos elegidos
     * @param database           base de desarrollo, o {@code null}
     * @param testDatabase       base de pruebas, o {@code null}
     * @param versionOverrides   versiones elegidas por clave {@code groupId:artifactId}
     * @param architecture       estilo, reparto en módulos y patrones
     * @param options            Javadoc, pruebas y formato de configuración
     */
    public ProjectSpec(final ProjectMetadata metadata, final BuildSettings build, final Set<String> dependencyGroupIds,
                       final DatabaseConnection database, final DatabaseConnection testDatabase,
                       final Map<String, String> versionOverrides, final ArchitectureSpec architecture,
                       final GenerationOptions options) {
        this(metadata, build, dependencyGroupIds, database, testDatabase, versionOverrides, architecture, options,
                SecuritySelection.none(), DomainModel.empty());
    }

    /**
     * Dónde guarda los datos el proyecto al ejecutarse.
     *
     * @return {@link Persistence#DATABASE} si hay base de desarrollo; si no,
     *         {@link Persistence#IN_MEMORY}
     */
    public Persistence persistence() {
        return database == null ? Persistence.IN_MEMORY : Persistence.DATABASE;
    }

    /**
     * Dónde guardan los datos las pruebas del proyecto.
     *
     * @return {@link Persistence#NONE} si no se generan pruebas,
     *         {@link Persistence#DATABASE} si hay base de pruebas y, si no,
     *         {@link Persistence#IN_MEMORY}
     */
    public Persistence testPersistence() {
        if (!options.unitTests()) {
            return Persistence.NONE;
        }
        return testDatabase == null ? Persistence.IN_MEMORY : Persistence.DATABASE;
    }

    /**
     * Devuelve las bases de datos configuradas: desarrollo y pruebas.
     *
     * @return lista inmutable de 0 a 2 conexiones, en ese orden
     */
    public List<DatabaseConnection> databases() {
        return Stream.of(database, testDatabase).filter(Objects::nonNull).toList();
    }

    /**
     * Construye la petición de selección de dependencias, agregando
     * automáticamente el grupo del driver de cada base de datos configurada.
     *
     * @return petición lista para el selector de dependencias
     */
    public SelectionRequest selectionRequest() {
        Set<String> groupIds = new HashSet<>(dependencyGroupIds);
        databases().forEach(connection -> groupIds.add(connection.engine().driverGroupId()));
        return new SelectionRequest(groupIds, build.buildTool(), build.reactive());
    }
}