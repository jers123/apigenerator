package org.jers.apigenerator.core.architecture;

/**
 * Relación entre un patrón de diseño y un estilo de arquitectura.
 */
public enum Compatibility {

    /** El estilo lo exige; se incluye siempre. */
    NATIVE,

    /** Encaja bien, pero es opcional. */
    COMPATIBLE,

    /** Rompe el estilo o lo vacía de sentido. */
    CONTRAINDICATED,

    /** No tiene sentido en un único proyecto, como Saga o API Gateway. */
    NOT_APPLICABLE;

    /**
     * Indica si el usuario puede tenerlo en su proyecto.
     *
     * @return {@code true} para nativo o compatible
     */
    public boolean selectable() {
        return this == NATIVE || this == COMPATIBLE;
    }

    /**
     * Traduce un símbolo de la matriz de {@link DesignPattern}.
     *
     * @param symbol {@code N} nativo, {@code C} compatible,
     *               {@code X} contraindicado, {@code -} no aplica
     * @return la compatibilidad correspondiente
     * @throws IllegalArgumentException si el símbolo no existe
     */
    static Compatibility fromSymbol(final char symbol) {
        return switch (symbol) {
            case 'N' -> NATIVE;
            case 'C' -> COMPATIBLE;
            case 'X' -> CONTRAINDICATED;
            case '-' -> NOT_APPLICABLE;
            default -> throw new IllegalArgumentException("Símbolo de compatibilidad desconocido: " + symbol);
        };
    }
}