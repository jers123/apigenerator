package org.jers.apigenerator.web.project;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jers.apigenerator.core.database.DatabaseEngine;
import org.jers.apigenerator.core.model.DomainModel;
import org.jers.apigenerator.core.project.ProjectSpec;
import org.jers.apigenerator.core.security.SecuritySelection;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Cuerpo de {@code POST /api/v1/projects/plan}: todo lo que el usuario eligió.
 *
 * @param metadata           identidad del proyecto
 * @param build              opciones de construcción
 * @param architecture       estilo, reparto en módulos y patrones
 * @param options            Javadoc, pruebas y formato de configuración
 * @param dependencyGroupIds ids de los grupos marcados
 * @param database           base principal, u omitida si no tiene
 * @param testDatabase       base de pruebas, u omitida si no tiene
 * @param versions           versiones elegidas por clave {@code groupId:artifactId};
 *                           omitida equivale a ninguna
 * @param security           seguridad elegida; omitida equivale a ninguna
 * @param model              modelo de dominio; omitido equivale a un modelo vacío
 */
public record ProjectRequest(
        @NotNull @Valid ProjectMetadataRequest metadata,
        @NotNull @Valid BuildSettingsRequest build,
        @NotNull @Valid ArchitectureRequest architecture,
        @NotNull @Valid GenerationOptionsRequest options,
        @NotNull Set<@NotBlank String> dependencyGroupIds,
        @Valid DatabaseConnectionRequest database,
        @Valid DatabaseConnectionRequest testDatabase,
        Map<String, String> versions,
        @Valid SecurityRequest security,
        DomainModel model) {

    /**
     * Convierte la petición en un proyecto del núcleo.
     *
     * @param engines busca un motor por su id
     * @return proyecto validado
     * @throws org.jers.apigenerator.core.common.DomainValidationException si
     *         algún dato no cumple las reglas del proyecto
     */
    public ProjectSpec toDomain(final Function<String, DatabaseEngine> engines) {
        return new ProjectSpec(
                metadata.toDomain(),
                build.toDomain(),
                dependencyGroupIds,
                database == null ? null : database.toDomain(engines),
                testDatabase == null ? null : testDatabase.toDomain(engines),
                versions == null ? Map.of() : versions,
                architecture.toDomain(),
                options.toDomain(),
                security == null ? SecuritySelection.none() : security.toDomain(),
                model);
    }
}