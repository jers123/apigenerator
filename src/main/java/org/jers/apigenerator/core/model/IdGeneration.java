package org.jers.apigenerator.core.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Cómo se genera el id de las entidades. Es la misma para todo el modelo.
 */
@Getter
@RequiredArgsConstructor
public enum IdGeneration {

    /** Autoincremento de la base de datos. */
    IDENTITY("Autoincremento (IDENTITY)", Set.of(ScalarType.INTEGER, ScalarType.LONG)),

    /** Secuencia de la base de datos; cada entidad tiene la suya. */
    SEQUENCE("Secuencia", Set.of(ScalarType.INTEGER, ScalarType.LONG)),

    /** UUID generado por Hibernate. */
    UUID("UUID generado", Set.of(ScalarType.UUID, ScalarType.STRING)),

    /** Sin generación: el valor lo asigna la aplicación. */
    ASSIGNED("Asignado por la aplicación",
            Set.of(ScalarType.STRING, ScalarType.INTEGER, ScalarType.LONG, ScalarType.UUID));

    /** Nombre legible. */
    private final String label;

    /** Tipos de id con los que funciona. */
    private final Set<ScalarType> compatibleTypes;

    /**
     * Indica si la estrategia funciona con un tipo de id.
     *
     * @param type tipo del id
     * @return {@code true} si es compatible
     */
    public boolean accepts(final ScalarType type) {
        return compatibleTypes.contains(type);
    }

    /**
     * Indica si necesita que el motor tenga secuencias.
     *
     * @return {@code true} solo para {@link #SEQUENCE}
     */
    public boolean needsSequences() {
        return this == SEQUENCE;
    }
}