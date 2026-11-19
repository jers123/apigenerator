package org.jers.apigenerator.core.database;

import org.jers.apigenerator.core.common.DomainValidationException;
import org.jers.apigenerator.core.common.Preconditions;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plantilla de URL JDBC con los marcadores {@code {host}}, {@code {port}} y
 * {@code {database}}.
 *
 * <p>Ejemplo: {@code jdbc:postgresql://{host}:{port}/{database}}.</p>
 */
final class JdbcUrlTemplate {

    /** Marcador del servidor. */
    private static final String HOST = "host";

    /** Marcador del puerto. */
    private static final String PORT = "port";

    /** Marcador del nombre de la base. */
    private static final String DATABASE = "database";

    /** Marcadores permitidos en una plantilla. */
    private static final Set<String> KNOWN = Set.of(HOST, PORT, DATABASE);

    /** Expresión que encuentra cada marcador {@code {nombre}}. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]*)}");

    /** Constructor privado: clase de utilidades sin estado. */
    private JdbcUrlTemplate() {
    }

    /**
     * Verifica que una plantilla sea utilizable.
     *
     * @param template plantilla a validar
     * @param embedded {@code true} si la base no usa servidor
     * @throws DomainValidationException si no empieza por {@code jdbc:}, usa
     *                                   marcadores desconocidos, no incluye
     *                                   {@code {database}} o, con servidor,
     *                                   no incluye {@code {host}} y {@code {port}}
     */
    static void validate(final String template, final boolean embedded) {
        Preconditions.requireText(template, "jdbcUrlTemplate");
        if (!template.startsWith("jdbc:")) {
            throw new DomainValidationException("jdbcUrlTemplate debe empezar por 'jdbc:'");
        }
        Set<String> found = new HashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(template);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!KNOWN.contains(name)) {
                throw new DomainValidationException(
                        "jdbcUrlTemplate usa el marcador desconocido {" + name + "}; use {host}, {port} o {database}");
            }
            found.add(name);
        }
        if (!found.contains(DATABASE)) {
            throw new DomainValidationException("jdbcUrlTemplate debe incluir {database}");
        }
        if (!embedded && !(found.contains(HOST) && found.contains(PORT))) {
            throw new DomainValidationException("Una base con servidor debe incluir {host} y {port} en jdbcUrlTemplate");
        }
    }

    /**
     * Reemplaza los marcadores por los datos de la conexión.
     *
     * @param template plantilla validada
     * @param host     servidor, o {@code null} en bases embebidas
     * @param port     puerto
     * @param database nombre de la base
     * @return URL JDBC final
     */
    static String render(final String template, final String host, final int port, final String database) {
        return template
                .replace("{" + HOST + "}", host == null ? "" : host)
                .replace("{" + PORT + "}", String.valueOf(port))
                .replace("{" + DATABASE + "}", database);
    }
}