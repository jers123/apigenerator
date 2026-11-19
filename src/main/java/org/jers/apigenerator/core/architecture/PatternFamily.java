package org.jers.apigenerator.core.architecture;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Catálogo de origen de un patrón de diseño, para agruparlos en la interfaz.
 */
@Getter
@RequiredArgsConstructor
public enum PatternFamily {

    /** Patrones GoF que crean objetos. */
    CREATIONAL("Creacionales (GoF)"),

    /** Patrones GoF que componen objetos. */
    STRUCTURAL("Estructurales (GoF)"),

    /** Patrones GoF que reparten responsabilidades. */
    BEHAVIORAL("De comportamiento (GoF)"),

    /** Patrones de aplicaciones empresariales (Fowler) y de DDD (Evans). */
    ENTERPRISE("Empresariales y de dominio"),

    /** Patrones que definen la forma de toda la aplicación. */
    ARCHITECTURAL("Arquitectónicos"),

    /** Patrones de sistemas distribuidos e integración. */
    DISTRIBUTED("Distribuidos e integración");

    /** Nombre legible. */
    private final String label;
}