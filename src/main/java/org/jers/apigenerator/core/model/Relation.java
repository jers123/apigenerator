package org.jers.apigenerator.core.model;

import java.util.Objects;

/**
 * Relación de una entidad con otra (o consigo misma). Se declara una sola
 * vez; si tiene nombre inverso, el lado opuesto aparece solo en el destino.
 *
 * @param id             id interno estable
 * @param name           nombre en camelCase, por ejemplo {@code cliente}
 * @param targetEntityId id interno de la entidad destino
 * @param cardinality    cardinalidad vista desde la entidad que la declara
 * @param required       si es obligatoria; solo vale en 1‑1 y N‑1
 * @param inverseName    nombre del lado inverso en el destino, o {@code null}
 *                       si es unidireccional
 * @param joinColumn     columna de la clave foránea escrita por el usuario, o
 *                       {@code null} para derivarla
 * @param joinTable      tabla intermedia escrita por el usuario (solo N‑N), o
 *                       {@code null} para derivarla
 */
public record Relation(
        String id,
        String name,
        String targetEntityId,
        Cardinality cardinality,
        boolean required,
        String inverseName,
        String joinColumn,
        String joinTable) {

    /**
     * Normaliza los textos.
     *
     * @throws org.jers.apigenerator.core.common.DomainValidationException si falta el id interno
     * @throws NullPointerException si falta la cardinalidad
     */
    public Relation {
        id = ModelText.internalId(id, "La relación '" + name + "'");
        name = ModelText.required(name);
        targetEntityId = ModelText.required(targetEntityId);
        Objects.requireNonNull(cardinality, "cardinality es obligatoria");
        inverseName = ModelText.optional(inverseName);
        joinColumn = ModelText.optional(joinColumn);
        joinTable = ModelText.optional(joinTable);
    }

    /**
     * Indica si la relación se navega desde ambos lados.
     *
     * @return {@code true} si tiene nombre inverso
     */
    public boolean bidirectional() {
        return inverseName != null;
    }
}