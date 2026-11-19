package org.jers.apigenerator.core.model;

import org.jers.apigenerator.core.common.DomainValidationException;

/**
 * Normaliza los textos del modelo. Los nombres vacíos no se rechazan aquí:
 * los informa el validador con su ubicación.
 */
final class ModelText {

    /** Constructor privado: clase de utilidades sin estado. */
    private ModelText() {
    }

    /**
     * Normaliza un texto obligatorio.
     *
     * @param value texto recibido
     * @return el texto sin espacios a los lados, o vacío si es {@code null}
     */
    static String required(final String value) {
        return value == null ? "" : value.strip();
    }

    /**
     * Normaliza un texto opcional.
     *
     * @param value texto recibido
     * @return el texto sin espacios a los lados, o {@code null} si está vacío
     */
    static String optional(final String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    /**
     * Exige el id interno de un elemento del modelo. Lo crea la pantalla y no
     * lo escribe el usuario, así que su falta es un error de la petición.
     *
     * @param value id recibido
     * @param owner qué elemento es, para el mensaje
     * @return el id sin espacios a los lados
     * @throws DomainValidationException si falta
     */
    static String internalId(final String value, final String owner) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(owner + " necesita un id interno");
        }
        return value.strip();
    }
}