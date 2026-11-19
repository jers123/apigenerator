package org.jers.apigenerator.core.project;

import org.jers.apigenerator.core.common.Preconditions;
import org.jers.apigenerator.core.database.DatabaseEngine;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Conexión a base de datos que tendrá el proyecto generado.
 *
 * @param engine       motor de base de datos
 * @param host         servidor; se ignora en bases embebidas
 * @param port         puerto; se ignora en bases embebidas
 * @param databaseName nombre de la base, o del servicio en Oracle
 * @param username     usuario de conexión
 * @param password     contraseña; puede estar vacía, nunca es {@code null}
 */
public record DatabaseConnection(
        DatabaseEngine engine,
        String host,
        int port,
        String databaseName,
        String username,
        String password) {

    /** Formato de nombre válido en las URL JDBC de todos los motores. */
    private static final Pattern DATABASE_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    /**
     * Valida la conexión según el tipo de motor.
     *
     * @throws org.jers.apigenerator.core.common.DomainValidationException si
     *         falta un dato obligatorio, el nombre de la base no es válido o
     *         el puerto está fuera de rango
     * @throws NullPointerException si el motor es {@code null}
     */
    public DatabaseConnection {
        Objects.requireNonNull(engine, "engine es obligatorio");
        Preconditions.requireFormat(databaseName, DATABASE_NAME, "databaseName",
                "debe empezar por letra y tener solo letras, números o _");
        Preconditions.requireText(username, "username");
        if (!engine.embedded()) {
            Preconditions.requireText(host, "host");
            Preconditions.requirePort(port, "port");
        }
        password = password == null ? "" : password;
    }

    /**
     * Devuelve la URL JDBC de esta conexión.
     *
     * @return URL lista para {@code spring.datasource.url}
     */
    public String jdbcUrl() {
        return engine.jdbcUrl(host, port, databaseName);
    }

    /**
     * Representación para logs y depuración, con la contraseña oculta.
     *
     * @return texto con todos los datos excepto la contraseña
     */
    @Override
    public String toString() {
        return "DatabaseConnection[engine=" + engine + ", host=" + host + ", port=" + port
                + ", databaseName=" + databaseName + ", username=" + username + ", password=****]";
    }
}