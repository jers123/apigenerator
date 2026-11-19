package org.jers.apigenerator.core.model;

import java.util.Locale;
import java.util.Set;

/**
 * Palabras que no se pueden usar, o conviene no usar, como nombres del modelo.
 */
public final class ReservedWords {

    /** Palabras reservadas y literales de Java, más las contextuales que confunden. */
    private static final Set<String> JAVA = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null", "var", "record", "yield", "sealed", "permits");

    /** Palabras reservadas comunes a PostgreSQL, MySQL, Oracle, SQL Server y H2. */
    private static final Set<String> SQL = Set.of(
            "all", "alter", "and", "as", "asc", "between", "by", "case", "check", "column", "comment",
            "constraint", "create", "current", "date", "default", "delete", "desc", "distinct", "drop", "else",
            "end", "exists", "foreign", "from", "grant", "group", "having", "in", "index", "inner", "insert",
            "into", "is", "join", "key", "left", "level", "like", "limit", "not", "null", "number", "offset",
            "on", "or", "order", "outer", "primary", "references", "revoke", "right", "role", "row", "rows",
            "schema", "select", "session", "set", "size", "table", "then", "time", "timestamp", "to",
            "transaction", "union", "unique", "update", "user", "value", "values", "when", "where");

    /** Clases que usa el código generado y con las que una entidad chocaría. */
    private static final Set<String> CLASS_NAMES = Set.of(
            "Object", "String", "Integer", "Long", "Double", "Float", "Short", "Byte", "Boolean", "Character",
            "Number", "Void", "Class", "Record", "Enum", "System", "Math", "Thread", "Exception", "Error",
            "Override", "List", "Map", "Set", "Collection", "Optional", "UUID", "BigDecimal", "LocalDate",
            "LocalDateTime", "LocalTime", "Instant", "Entity", "Table", "Column", "Id", "Version",
            "Repository", "Service", "Component", "Controller", "RestController", "Page", "Pageable");

    /** Constructor privado: clase de utilidades sin estado. */
    private ReservedWords() {
    }

    /**
     * Indica si un nombre es palabra reservada de Java.
     *
     * @param name nombre de atributo o relación
     * @return {@code true} si no se puede usar como identificador
     */
    public static boolean javaKeyword(final String name) {
        return JAVA.contains(name);
    }

    /**
     * Indica si un nombre de tabla o columna es palabra reservada de SQL.
     *
     * @param name nombre en la base, sin importar mayúsculas
     * @return {@code true} si conviene usar otro
     */
    public static boolean sqlReserved(final String name) {
        return SQL.contains(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Indica si un nombre de entidad o superclase choca con una clase que usa el código generado.
     *
     * @param name nombre en PascalCase
     * @return {@code true} si el código generado no compilaría o sería ambiguo
     */
    public static boolean reservedClassName(final String name) {
        return CLASS_NAMES.contains(name);
    }
}