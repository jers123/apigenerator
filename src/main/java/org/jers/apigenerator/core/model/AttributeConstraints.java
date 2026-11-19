package org.jers.apigenerator.core.model;

import java.math.BigDecimal;

/**
 * Restricciones de un atributo; cada una es opcional. Qué restricciones
 * admite cada tipo y si son coherentes lo revisa el validador.
 *
 * @param minLength      longitud mínima de un texto, o {@code null}
 * @param maxLength      longitud máxima de un texto, o {@code null}
 * @param min            valor mínimo de un número, o {@code null}
 * @param max            valor máximo de un número, o {@code null}
 * @param integerDigits  dígitos enteros de un decimal, o {@code null}
 * @param fractionDigits dígitos decimales de un decimal, o {@code null}
 * @param pattern        expresión regular que debe cumplir un texto, o {@code null}
 * @param email          si un texto debe ser un correo
 * @param temporal       restricción de tiempo de una fecha, o {@code null}
 */
public record AttributeConstraints(
        Integer minLength,
        Integer maxLength,
        BigDecimal min,
        BigDecimal max,
        Integer integerDigits,
        Integer fractionDigits,
        String pattern,
        boolean email,
        TemporalRule temporal) {

    /**
     * Normaliza el patrón: vacío equivale a no tenerlo.
     */
    public AttributeConstraints {
        pattern = ModelText.optional(pattern);
    }

    /**
     * Sin restricciones.
     *
     * @return restricciones vacías
     */
    public static AttributeConstraints none() {
        return new AttributeConstraints(null, null, null, null, null, null, null, false, null);
    }

    /**
     * Indica si hay al menos una restricción; si hay, el proyecto necesita
     * Jakarta Validation.
     *
     * @return {@code true} si alguna está definida
     */
    public boolean hasAny() {
        return minLength != null || maxLength != null || min != null || max != null || integerDigits != null
                || fractionDigits != null || pattern != null || email || temporal != null;
    }
}