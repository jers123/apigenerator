package org.jers.apigenerator.core.model;

import org.jers.apigenerator.core.common.DomainValidationException;

/**
 * Cómo se guarda una relación en una base relacional: qué entidad es la
 * dueña, la columna de clave foránea y, en N‑N, la tabla intermedia.
 *
 * @param relationId        id interno de la relación
 * @param ownerEntityId     entidad cuya tabla tiene la clave foránea o que es
 *                          dueña de la tabla intermedia
 * @param joinColumn        columna de la clave foránea; en N‑N, la del lado dueño
 * @param joinTable         tabla intermedia, solo en N‑N; si no, {@code null}
 * @param inverseJoinColumn columna del otro lado en la tabla intermedia, solo
 *                          en N‑N; si no, {@code null}
 */
public record RelationMapping(
        String relationId,
        String ownerEntityId,
        String joinColumn,
        String joinTable,
        String inverseJoinColumn) {

    /**
     * Valida que el mapeo sea completo y coherente.
     *
     * @throws DomainValidationException si falta un dato o la tabla intermedia
     *                                   viene sin su segunda columna, o al revés
     */
    public RelationMapping {
        if (relationId == null || ownerEntityId == null || joinColumn == null) {
            throw new DomainValidationException("Un mapeo de relación necesita relación, entidad dueña y columna");
        }
        if ((joinTable == null) != (inverseJoinColumn == null)) {
            throw new DomainValidationException("La tabla intermedia y su segunda columna van juntas");
        }
    }

    /**
     * Indica si la relación se guarda en una tabla intermedia.
     *
     * @return {@code true} en las relaciones N‑N
     */
    public boolean usesJoinTable() {
        return joinTable != null;
    }
}