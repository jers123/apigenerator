package org.jers.apigenerator.web.project;

import org.jers.apigenerator.core.project.DatabaseConnection;

/**
 * Conexión del plan, sin la contraseña.
 *
 * @param engineId id del motor
 * @param jdbcUrl  URL JDBC final
 * @param username usuario de conexión
 */
public record ConnectionResponse(String engineId, String jdbcUrl, String username) {

    /**
     * Convierte una conexión del núcleo; la contraseña nunca sale por la API.
     *
     * @param connection conexión, o {@code null}
     * @return representación para la API, o {@code null} si no hay conexión
     */
    public static ConnectionResponse from(final DatabaseConnection connection) {
        return connection == null
                ? null
                : new ConnectionResponse(connection.engine().id(), connection.jdbcUrl(), connection.username());
    }
}