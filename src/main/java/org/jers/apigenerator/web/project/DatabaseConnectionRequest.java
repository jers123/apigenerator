package org.jers.apigenerator.web.project;

import jakarta.validation.constraints.NotBlank;
import org.jers.apigenerator.core.database.DatabaseEngine;
import org.jers.apigenerator.core.project.DatabaseConnection;

import java.util.function.Function;

/**
 * Conexión a base de datos en la petición; el motor se indica por su id.
 *
 * @param engineId     id del motor
 * @param host         servidor; se ignora en bases embebidas
 * @param port         puerto; se ignora en bases embebidas
 * @param databaseName nombre de la base
 * @param username     usuario
 * @param password     contraseña, opcional
 */
public record DatabaseConnectionRequest(
        @NotBlank String engineId,
        String host,
        Integer port,
        String databaseName,
        String username,
        String password) {

    /**
     * Convierte la petición en una conexión del núcleo.
     *
     * @param engines busca un motor por su id
     * @return conexión validada
     * @throws org.jers.apigenerator.core.common.DomainValidationException si
     *         el motor no existe o algún dato no es válido
     */
    public DatabaseConnection toDomain(final Function<String, DatabaseEngine> engines) {
        return new DatabaseConnection(engines.apply(engineId), host, port == null ? 0 : port,
                databaseName, username, password);
    }
}