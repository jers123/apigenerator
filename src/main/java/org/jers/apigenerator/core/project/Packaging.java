package org.jers.apigenerator.core.project;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Formato de empaquetado del proyecto generado.
 */
@Getter
@RequiredArgsConstructor
public enum Packaging {

    /** JAR ejecutable con servidor embebido; la opción habitual en Spring Boot. */
    JAR("jar"),

    /** WAR desplegable en un servidor de aplicaciones externo. */
    WAR("war");

    /** Valor de la etiqueta {@code <packaging>} del {@code pom.xml}. */
    private final String value;
}