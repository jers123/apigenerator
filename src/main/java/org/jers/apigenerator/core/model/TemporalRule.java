package org.jers.apigenerator.core.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Restricción de tiempo de una fecha o instante; se traduce a las anotaciones
 * de Jakarta Validation del mismo nombre.
 */
@Getter
@RequiredArgsConstructor
public enum TemporalRule {

    /** Estrictamente en el pasado ({@code @Past}). */
    PAST("En el pasado"),

    /** En el pasado o ahora ({@code @PastOrPresent}). */
    PAST_OR_PRESENT("En el pasado o ahora"),

    /** Estrictamente en el futuro ({@code @Future}). */
    FUTURE("En el futuro"),

    /** En el futuro o ahora ({@code @FutureOrPresent}). */
    FUTURE_OR_PRESENT("En el futuro o ahora");

    /** Nombre legible. */
    private final String label;
}