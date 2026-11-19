package org.jers.apigenerator.core.model;

import org.jers.apigenerator.core.common.DomainValidationException;

import java.util.Objects;

/**
 * Lado inverso de una relación bidireccional, tal como aparece en la entidad
 * destino. No se declara: se deriva de la relación original.
 *
 * @param name           nombre en la entidad destino, por ejemplo {@code pedidos}
 * @param sourceEntityId id interno de la entidad que declaró la relación
 * @param relationId     id interno de la relación original
 * @param cardinality    cardinalidad vista desde la entidad destino
 */
public record InverseRelation(String name, String sourceEntityId, String relationId, Cardinality cardinality) {

    /**
     * Valida que estén todos los datos.
     *
     * @throws DomainValidationException si falta el nombre o algún id
     * @throws NullPointerException      si falta la cardinalidad
     */
    public InverseRelation {
        if (name == null || name.isBlank() || sourceEntityId == null || relationId == null) {
            throw new DomainValidationException("Una relación inversa necesita nombre, entidad de origen y relación");
        }
        Objects.requireNonNull(cardinality, "cardinality es obligatoria");
    }
}