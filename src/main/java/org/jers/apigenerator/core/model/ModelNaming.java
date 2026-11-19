package org.jers.apigenerator.core.model;

/**
 * Convierte los nombres del modelo en nombres de tabla, columna y ruta REST.
 * Solo transforma texto; no valida: un nombre inválido produce un resultado
 * que después marca el validador.
 */
public final class ModelNaming {

    /** Sufijo de las columnas de clave foránea. */
    public static final String ID_SUFFIX = "_id";

    /** Sufijo de los nombres de secuencia. */
    public static final String SEQUENCE_SUFFIX = "_seq";

    /** Vocales que forman el plural con {@code s}. */
    private static final String VOWELS = "aeiou";

    /** Constructor privado: clase de utilidades sin estado. */
    private ModelNaming() {
    }

    /**
     * Convierte PascalCase o camelCase en snake_case. Las siglas se tratan
     * como una palabra: {@code URLCorta} da {@code url_corta}.
     *
     * @param name nombre del modelo, por ejemplo {@code DetallePedido}
     * @return el nombre en snake_case, por ejemplo {@code detalle_pedido}
     */
    public static String snakeCase(final String name) {
        return separate(name, '_');
    }

    /**
     * Convierte PascalCase o camelCase en kebab-case.
     *
     * @param name nombre del modelo, por ejemplo {@code DetallePedido}
     * @return el nombre en kebab-case, por ejemplo {@code detalle-pedido}
     */
    public static String kebabCase(final String name) {
        return separate(name, '-');
    }

    /**
     * Plural en español con reglas simples: vocal más {@code s}, {@code z}
     * cambia a {@code ces} y cualquier otra consonante más {@code es}. Las
     * excepciones se corrigen escribiendo la ruta a mano.
     *
     * @param word palabra en minúsculas
     * @return el plural, o vacío si la palabra está vacía
     */
    public static String plural(final String word) {
        if (word == null || word.isEmpty()) {
            return "";
        }
        char last = word.charAt(word.length() - 1);
        if (VOWELS.indexOf(last) >= 0) {
            return word + "s";
        }
        if (last == 'z') {
            return word.substring(0, word.length() - 1) + "ces";
        }
        return word + "es";
    }

    /**
     * Ruta REST de una entidad: kebab-case con la última palabra en plural.
     *
     * @param entityName nombre de la entidad, por ejemplo {@code DetallePedido}
     * @return la ruta, por ejemplo {@code /detalle-pedidos}; {@code /} si el nombre está vacío
     */
    public static String resourcePath(final String entityName) {
        String kebab = kebabCase(entityName);
        int lastDash = kebab.lastIndexOf('-');
        return "/" + kebab.substring(0, lastDash + 1) + plural(kebab.substring(lastDash + 1));
    }

    /**
     * Separa las palabras de un nombre con mayúsculas internas y lo pasa a minúsculas.
     *
     * @param name      nombre a convertir; {@code null} equivale a vacío
     * @param separator carácter entre palabras
     * @return el nombre convertido
     */
    private static String separate(final String name, final char separator) {
        String text = name == null ? "" : name.strip();
        StringBuilder result = new StringBuilder(text.length() + 4);
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (i > 0 && Character.isUpperCase(current) && startsWord(text, i)) {
                result.append(separator);
            }
            result.append(Character.toLowerCase(current));
        }
        return result.toString();
    }

    /**
     * Indica si la mayúscula de una posición empieza una palabra nueva.
     *
     * @param text  nombre completo
     * @param index posición de una mayúscula que no es la primera letra
     * @return {@code true} si viene después de una minúscula o un dígito, o si
     *         cierra una sigla porque le sigue una minúscula
     */
    private static boolean startsWord(final String text, final int index) {
        char previous = text.charAt(index - 1);
        boolean nextIsLower = index + 1 < text.length() && Character.isLowerCase(text.charAt(index + 1));
        return Character.isLowerCase(previous) || Character.isDigit(previous)
                || (Character.isUpperCase(previous) && nextIsLower);
    }
}