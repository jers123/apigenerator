package org.jers.apigenerator.core.model;

import java.util.Objects;

/**
 * Id de todas las entidades del modelo: un solo tipo, una sola forma de
 * generarlo y un solo lugar donde se declara. La compatibilidad entre tipo y
 * generación la revisa el validador.
 *
 * @param type       tipo del id
 * @param generation cómo se genera
 * @param placement  dónde se declaran el id y la auditoría; {@code null}
 *                   equivale a {@link IdPlacement#ENTITY}
 */
public record IdSettings(ScalarType type, IdGeneration generation, IdPlacement placement) {

    /**
     * Valida que estén el tipo y la generación, y completa la ubicación.
     *
     * @throws NullPointerException si falta el tipo o la generación
     */
    public IdSettings {
        Objects.requireNonNull(type, "type es obligatorio");
        Objects.requireNonNull(generation, "generation es obligatoria");
        placement = placement == null ? IdPlacement.ENTITY : placement;
    }

    /**
     * Crea la configuración con el id en cada entidad.
     *
     * @param type       tipo del id
     * @param generation cómo se genera
     */
    public IdSettings(final ScalarType type, final IdGeneration generation) {
        this(type, generation, IdPlacement.ENTITY);
    }

    /**
     * Id por defecto: {@code Long} con autoincremento, en cada entidad.
     *
     * @return configuración por defecto
     */
    public static IdSettings defaults() {
        return new IdSettings(ScalarType.LONG, IdGeneration.IDENTITY, IdPlacement.ENTITY);
    }
}