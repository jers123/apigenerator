package org.jers.apigenerator.core.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Cardinalidad de una relación, vista desde la entidad que la declara.
 */
@Getter
@RequiredArgsConstructor
public enum Cardinality {

    /** Uno a uno: la entidad que declara tiene la clave foránea. */
    ONE_TO_ONE("1‑1"),

    /** Muchos a uno: la entidad que declara tiene la clave foránea. */
    MANY_TO_ONE("N‑1"),

    /** Uno a muchos: la clave foránea está en la entidad destino. */
    ONE_TO_MANY("1‑N"),

    /** Muchos a muchos: tabla intermedia; la entidad que declara es la dueña. */
    MANY_TO_MANY("N‑N");

    /** Nombre corto para la interfaz. */
    private final String label;

    /**
     * Indica si del lado que declara la relación hay una colección.
     *
     * @return {@code true} para 1‑N y N‑N
     */
    public boolean toMany() {
        return this == ONE_TO_MANY || this == MANY_TO_MANY;
    }

    /**
     * Indica si la relación puede ser obligatoria.
     *
     * @return {@code true} para 1‑1 y N‑1; en las colecciones no tiene sentido
     */
    public boolean allowsRequired() {
        return !toMany();
    }

    /**
     * Indica si la entidad que declara la relación es la dueña.
     *
     * @return {@code false} solo para 1‑N, cuya clave foránea está en el destino
     */
    public boolean declaringSideOwns() {
        return this != ONE_TO_MANY;
    }

    /**
     * Cardinalidad vista desde la entidad destino, para el lado inverso.
     *
     * @return la cardinalidad opuesta
     */
    public Cardinality inverse() {
        return switch (this) {
            case ONE_TO_ONE -> ONE_TO_ONE;
            case MANY_TO_ONE -> ONE_TO_MANY;
            case ONE_TO_MANY -> MANY_TO_ONE;
            case MANY_TO_MANY -> MANY_TO_MANY;
        };
    }
}