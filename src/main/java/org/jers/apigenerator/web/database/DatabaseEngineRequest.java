package org.jers.apigenerator.web.database;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jers.apigenerator.core.catalog.DependencyGroup;
import org.jers.apigenerator.core.database.DatabaseEngine;
import org.jers.apigenerator.core.database.DatabaseEngineDefinition;
import org.jers.apigenerator.core.database.DatabaseKind;
import org.jers.apigenerator.web.catalog.GroupDefinition;

/**
 * Cuerpo de {@code POST /api/v1/databases} y {@code PUT /api/v1/databases/{id}}:
 * el motor y el grupo del catálogo que aporta su driver.
 *
 * @param id                identificador; al editar debe coincidir con el de la URL
 * @param label             nombre legible
 * @param embedded          si corre dentro de la aplicación, sin servidor
 * @param defaultPort       puerto por defecto; se ignora si es embebida
 * @param defaultUsername   usuario por defecto
 * @param jdbcUrlTemplate   plantilla con {@code {host}}, {@code {port}} y {@code {database}}
 * @param driverGroup       grupo de base de datos con el driver; su id pasa a
 *                          ser el {@code driverGroupId} del motor
 * @param kind              tipo de base; omitido equivale a relacional
 * @param supportsSequences si el motor tiene secuencias
 */
public record DatabaseEngineRequest(
        @NotBlank String id,
        @NotBlank String label,
        boolean embedded,
        Integer defaultPort,
        @NotBlank String defaultUsername,
        @NotBlank String jdbcUrlTemplate,
        @NotNull @Valid GroupDefinition driverGroup,
        DatabaseKind kind,
        boolean supportsSequences) {

    /**
     * Convierte la petición en un motor con su grupo; el núcleo aplica sus
     * propias reglas.
     *
     * @return motor y grupo validados
     * @throws org.jers.apigenerator.core.common.DomainValidationException si
     *         los datos no cumplen las reglas del motor o del grupo
     */
    public DatabaseEngineDefinition toDomain() {
        DependencyGroup group = driverGroup.toDomain();
        return new DatabaseEngineDefinition(
                new DatabaseEngine(id, label, group.id(), embedded, defaultPort, defaultUsername, jdbcUrlTemplate,
                        kind, supportsSequences),
                group);
    }
}