package org.jers.apigenerator.core.common;

import java.util.regex.Pattern;

/**
 * Validaciones reutilizables por los tipos del núcleo.
 *
 * <p>Todas lanzan {@link DomainValidationException} con un mensaje que nombra
 * el campo, para que el error llegue claro hasta la API y la interfaz.</p>
 */
public final class Preconditions {

    /** Mayor número de puerto TCP válido. */
    private static final int MAX_PORT = 65_535;

    /** Identificador estable: minúsculas y números, con guiones simples entre palabras. */
    private static final Pattern SLUG = Pattern.compile("[a-z][a-z0-9]*(-[a-z0-9]+)*");

    /** Constructor privado: clase de utilidades sin estado. */
    private Preconditions() {
    }

    /**
     * Verifica que un texto obligatorio tenga contenido.
     *
     * @param value valor a validar
     * @param field nombre del campo, usado en el mensaje de error
     * @return el mismo valor recibido
     * @throws DomainValidationException si el valor es {@code null} o está en blanco
     */
    public static String requireText(final String value, final String field) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(field + " es obligatorio y no puede estar en blanco");
        }
        return value;
    }

    /**
     * Verifica que un texto obligatorio cumpla un formato completo.
     *
     * @param value    valor a validar
     * @param pattern  expresión que debe cumplir el valor completo
     * @param field    nombre del campo
     * @param expected descripción legible del formato esperado
     * @return el mismo valor recibido
     * @throws DomainValidationException si el valor está en blanco o no cumple el formato
     */
    public static String requireFormat(final String value, final Pattern pattern,
                                       final String field, final String expected) {
        requireText(value, field);
        if (!pattern.matcher(value).matches()) {
            throw new DomainValidationException(field + " '" + value + "' no es válido: " + expected);
        }
        return value;
    }

    /**
     * Verifica que un número sea un puerto TCP válido.
     *
     * @param port  valor a validar
     * @param field nombre del campo
     * @return el mismo valor recibido
     * @throws DomainValidationException si no está entre 1 y 65535
     */
    public static int requirePort(final int port, final String field) {
        if (port < 1 || port > MAX_PORT) {
            throw new DomainValidationException(field + " debe estar entre 1 y " + MAX_PORT + ", pero es " + port);
        }
        return port;
    }

    /**
     * Verifica que un texto sea un identificador estable válido.
     *
     * @param value valor a validar
     * @param field nombre del campo
     * @return el mismo valor recibido
     * @throws DomainValidationException si está en blanco o no cumple el formato
     */
    public static String requireSlug(final String value, final String field) {
        return requireFormat(value, SLUG, field, "use minúsculas, números y guiones, por ejemplo spring-web");
    }
}