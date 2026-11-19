package org.jers.apigenerator.core.model;

import org.jers.apigenerator.core.common.DomainValidationException;

/**
 * Tipo de un atributo: un tipo simple o, más adelante, un enum del modelo.
 * Las entidades no son tipos de atributo: apuntar a otra entidad es una
 * {@link Relation}.
 *
 * @param scalar tipo simple, o {@code null} si es un enum
 * @param enumId id interno del enum del modelo, o {@code null} si es simple
 */
public record AttributeType(ScalarType scalar, String enumId) {

    /**
     * Exige exactamente uno de los dos.
     *
     * @throws DomainValidationException si faltan los dos o vienen ambos
     */
    public AttributeType {
        enumId = ModelText.optional(enumId);
        if ((scalar == null) == (enumId == null)) {
            throw new DomainValidationException("El tipo de un atributo debe ser un tipo simple o un enum, uno solo");
        }
    }

    /**
     * Crea un tipo simple.
     *
     * @param scalar tipo simple
     * @return el tipo
     */
    public static AttributeType of(final ScalarType scalar) {
        return new AttributeType(scalar, null);
    }

    /**
     * Indica si el tipo es un enum del modelo.
     *
     * @return {@code true} si apunta a un enum
     */
    public boolean usesEnum() {
        return enumId != null;
    }
}