package org.jers.apigenerator.web.database;

import org.jers.apigenerator.core.database.DatabaseEngine;
import org.jers.apigenerator.core.database.DatabaseKind;

/**
 * Representación JSON de un motor de base de datos.
 *
 * @param id                identificador estable
 * @param label             nombre legible
 * @param driverGroupId     grupo del catálogo que aporta el driver
 * @param embedded          si corre dentro de la aplicación, sin servidor
 * @param defaultPort       puerto por defecto, o {@code null} si es embebida
 * @param defaultUsername   usuario por defecto
 * @param jdbcUrlTemplate   plantilla de URL
 * @param kind              tipo de base
 * @param supportsSequences si el motor tiene secuencias
 * @param exampleUrl        URL de ejemplo con {@code localhost}, el puerto por
 *                          defecto y la base {@code mi_base}, para revisar la plantilla
 */
public record DatabaseEngineResponse(
        String id,
        String label,
        String driverGroupId,
        boolean embedded,
        Integer defaultPort,
        String defaultUsername,
        String jdbcUrlTemplate,
        DatabaseKind kind,
        boolean supportsSequences,
        String exampleUrl) {

    /** Servidor usado en la URL de ejemplo. */
    private static final String EXAMPLE_HOST = "localhost";

    /** Base usada en la URL de ejemplo. */
    private static final String EXAMPLE_DATABASE = "mi_base";

    /**
     * Convierte un motor del núcleo a su representación JSON.
     *
     * @param engine motor del núcleo
     * @return representación para la API
     */
    public static DatabaseEngineResponse from(final DatabaseEngine engine) {
        int port = engine.defaultPort() == null ? 0 : engine.defaultPort();
        return new DatabaseEngineResponse(
                engine.id(),
                engine.label(),
                engine.driverGroupId(),
                engine.embedded(),
                engine.defaultPort(),
                engine.defaultUsername(),
                engine.jdbcUrlTemplate(),
                engine.kind(),
                engine.supportsSequences(),
                engine.jdbcUrl(EXAMPLE_HOST, port, EXAMPLE_DATABASE));
    }
}