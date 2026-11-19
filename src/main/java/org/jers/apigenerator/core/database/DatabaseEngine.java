package org.jers.apigenerator.core.database;

import org.jers.apigenerator.core.common.DomainValidationException;
import org.jers.apigenerator.core.common.Preconditions;

/**
 * Motor de base de datos disponible para el proyecto generado.
 *
 * <p>No se configura el dialecto de Hibernate: Spring Boot lo detecta a
 * partir de la conexión.</p>
 *
 * @param id                identificador estable, en minúsculas con guiones
 *                          (por ejemplo {@code sql-server}); no cambia al editar
 * @param label             nombre legible para la interfaz
 * @param driverGroupId     id del grupo del catálogo que aporta el driver
 * @param embedded          {@code true} si corre dentro de la aplicación, sin
 *                          servidor, host ni puerto
 * @param defaultPort       puerto por defecto; {@code null} en bases embebidas
 * @param defaultUsername   usuario por defecto de una instalación local
 * @param jdbcUrlTemplate   plantilla de URL con {@code {host}}, {@code {port}}
 *                          y {@code {database}}
 * @param kind              tipo de base; {@code null} equivale a
 *                          {@link DatabaseKind#RELATIONAL}
 * @param supportsSequences si el motor tiene secuencias, necesarias para la
 *                          estrategia de id {@code SEQUENCE}
 */
public record DatabaseEngine(
        String id,
        String label,
        String driverGroupId,
        boolean embedded,
        Integer defaultPort,
        String defaultUsername,
        String jdbcUrlTemplate,
        DatabaseKind kind,
        boolean supportsSequences) {

    /**
     * Valida el motor.
     *
     * @throws DomainValidationException si el identificador o la plantilla no
     *                                   son válidos, falta un texto
     *                                   obligatorio, una base con servidor
     *                                   no tiene un puerto válido o el tipo
     *                                   de base aún no está soportado
     */
    public DatabaseEngine {
        Preconditions.requireSlug(id, "id");
        Preconditions.requireText(label, "label");
        Preconditions.requireText(driverGroupId, "driverGroupId");
        Preconditions.requireText(defaultUsername, "defaultUsername");
        kind = kind == null ? DatabaseKind.RELATIONAL : kind;
        if (!kind.isSupported()) {
            throw new DomainValidationException("Los motores de tipo " + kind.getLabel()
                    + " todavía no están soportados: el generador solo produce proyectos relacionales");
        }
        JdbcUrlTemplate.validate(jdbcUrlTemplate, embedded);
        if (embedded) {
            defaultPort = null;
        } else {
            if (defaultPort == null) {
                throw new DomainValidationException("Una base con servidor necesita defaultPort");
            }
            Preconditions.requirePort(defaultPort, "defaultPort");
        }
    }

    /**
     * Crea un motor relacional sin secuencias. Mantiene compatibles los usos
     * anteriores a los campos {@code kind} y {@code supportsSequences}.
     *
     * @param id              identificador estable
     * @param label           nombre legible
     * @param driverGroupId   id del grupo que aporta el driver
     * @param embedded        si corre dentro de la aplicación
     * @param defaultPort     puerto por defecto, o {@code null} si es embebida
     * @param defaultUsername usuario por defecto
     * @param jdbcUrlTemplate plantilla de URL
     */
    public DatabaseEngine(final String id, final String label, final String driverGroupId, final boolean embedded,
                          final Integer defaultPort, final String defaultUsername, final String jdbcUrlTemplate) {
        this(id, label, driverGroupId, embedded, defaultPort, defaultUsername, jdbcUrlTemplate,
                DatabaseKind.RELATIONAL, false);
    }

    /**
     * Construye la URL JDBC de una conexión a partir de la plantilla.
     *
     * @param host         servidor; se ignora en bases embebidas
     * @param port         puerto; se ignora en bases embebidas
     * @param databaseName nombre de la base, o del servicio en Oracle
     * @return URL lista para {@code spring.datasource.url}
     */
    public String jdbcUrl(final String host, final int port, final String databaseName) {
        return JdbcUrlTemplate.render(jdbcUrlTemplate, host, port, databaseName);
    }
}