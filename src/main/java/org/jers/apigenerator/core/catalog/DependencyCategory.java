package org.jers.apigenerator.core.catalog;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DependencyCategory {

    /** Herramientas y plugins exclusivos de proyectos Maven. */
    MAVEN("Maven"),

    /** Dependencias exclusivas de proyectos Gradle. */
    GRADLE("Gradle"),

    /** Starters y módulos del ecosistema Spring. */
    SPRING("Spring"),

    /** Drivers JDBC y R2DBC y bases de datos embebidas. */
    DATABASE("Database"),

    /** Librerías de propósito general (Lombok, Mockito, SpringDoc...). */
    OTHER("Other");

    /** Nombre legible que se muestra en la interfaz. */
    private final String label;
}