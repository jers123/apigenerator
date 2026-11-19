package org.jers.apigenerator.core.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tipo de dato simple de un atributo, con lo que el generador necesita
 * saber de él: su clase Java, si sirve como id, qué restricciones admite y
 * un valor de ejemplo fijo para las pruebas generadas.
 */
@Getter
@RequiredArgsConstructor
public enum ScalarType {

    /** Texto. */
    STRING("String", null, "\"texto\"", Family.TEXT, true),

    /** Entero de 32 bits. */
    INTEGER("Integer", null, "1", Family.INTEGRAL, true),

    /** Entero de 64 bits. */
    LONG("Long", null, "1L", Family.INTEGRAL, true),

    /** Decimal exacto, para dinero y cantidades. */
    BIG_DECIMAL("BigDecimal", "java.math.BigDecimal", "new BigDecimal(\"10.50\")", Family.DECIMAL, false),

    /** Decimal de punto flotante. */
    DOUBLE("Double", null, "1.0", Family.FLOATING, false),

    /** Verdadero o falso. */
    BOOLEAN("Boolean", null, "true", Family.BOOLEAN, false),

    /** Fecha sin hora. */
    LOCAL_DATE("LocalDate", "java.time.LocalDate", "LocalDate.of(2026, 1, 15)", Family.TEMPORAL, false),

    /** Fecha y hora sin zona. */
    LOCAL_DATE_TIME("LocalDateTime", "java.time.LocalDateTime", "LocalDateTime.of(2026, 1, 15, 10, 30)",
            Family.TEMPORAL, false),

    /** Hora sin fecha. */
    LOCAL_TIME("LocalTime", "java.time.LocalTime", "LocalTime.of(10, 30)", Family.TIME, false),

    /** Instante en UTC. */
    INSTANT("Instant", "java.time.Instant", "Instant.parse(\"2026-01-15T10:30:00Z\")", Family.TEMPORAL, false),

    /** Identificador universal. */
    UUID("UUID", "java.util.UUID", "UUID.fromString(\"3f2504e0-4f89-11d3-9a0c-0305e82c3301\")",
            Family.IDENTIFIER, true);

    /** Nombre simple de la clase Java. */
    private final String javaType;

    /** Import necesario, o {@code null} si es de {@code java.lang}. */
    private final String importName;

    /** Expresión Java con un valor fijo, para pruebas y datos de ejemplo. */
    private final String exampleValue;

    /** Familia, que decide las restricciones que admite. */
    private final Family family;

    /** Si puede ser el tipo del id de las entidades. */
    private final boolean idCapable;

    /**
     * Indica si admite longitud, patrón y email.
     *
     * @return {@code true} solo para textos
     */
    public boolean acceptsTextRules() {
        return family == Family.TEXT;
    }

    /**
     * Indica si admite mínimo y máximo.
     *
     * @return {@code true} para números enteros y decimales
     */
    public boolean acceptsRange() {
        return family == Family.INTEGRAL || family == Family.DECIMAL || family == Family.FLOATING;
    }

    /**
     * Indica si admite dígitos enteros y decimales.
     *
     * @return {@code true} solo para decimales exactos
     */
    public boolean acceptsDigits() {
        return family == Family.DECIMAL;
    }

    /**
     * Indica si admite pasado o futuro.
     *
     * @return {@code true} para fechas e instantes
     */
    public boolean acceptsTemporalRule() {
        return family == Family.TEMPORAL;
    }

    /**
     * Familia de tipos, según las restricciones que tienen sentido.
     */
    public enum Family {

        /** Textos. */
        TEXT,

        /** Enteros. */
        INTEGRAL,

        /** Decimales exactos. */
        DECIMAL,

        /** Decimales de punto flotante. */
        FLOATING,

        /** Verdadero o falso. */
        BOOLEAN,

        /** Fechas, fechas con hora e instantes. */
        TEMPORAL,

        /** Horas sin fecha. */
        TIME,

        /** Identificadores universales. */
        IDENTIFIER
    }
}