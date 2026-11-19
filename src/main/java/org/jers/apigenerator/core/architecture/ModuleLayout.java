package org.jers.apigenerator.core.architecture;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Cómo se reparten las partes de la arquitectura en el proyecto generado.
 */
@Getter
@RequiredArgsConstructor
public enum ModuleLayout {

    /** Un solo módulo de build; las partes son paquetes. */
    SINGLE_MODULE("Un solo módulo (paquetes)"),

    /**
     * Un módulo de build por parte (subproyectos de Gradle o módulos de
     * Maven); el compilador impide las dependencias prohibidas.
     */
    MULTI_MODULE("Varios módulos (subproyectos)");

    /** Nombre legible. */
    private final String label;
}