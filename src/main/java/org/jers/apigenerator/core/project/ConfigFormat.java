package org.jers.apigenerator.core.project;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Formato de los archivos de configuración del proyecto generado.
 */
@Getter
@RequiredArgsConstructor
public enum ConfigFormat {

    /** {@code application.properties}: una clave por línea. */
    PROPERTIES("properties"),

    /** {@code application.yml}: claves anidadas por indentación. */
    YAML("yml");

    /** Extensión del archivo. */
    private final String extension;
}