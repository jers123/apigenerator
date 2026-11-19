package org.jers.apigenerator.web.project;

import org.jers.apigenerator.core.architecture.ArchitectureSpec;
import org.jers.apigenerator.core.architecture.ArchitectureStyle;
import org.jers.apigenerator.core.architecture.DesignPattern;
import org.jers.apigenerator.core.architecture.ModuleLayout;

import java.util.List;

/**
 * Arquitectura del plan, con los patrones nativos ya incluidos.
 *
 * @param style    estilo de arquitectura
 * @param layout   reparto en módulos
 * @param patterns patrones finales, en el orden del catálogo
 */
public record ArchitectureResponse(ArchitectureStyle style, ModuleLayout layout, List<DesignPattern> patterns) {

    /**
     * Convierte la arquitectura del núcleo.
     *
     * @param spec arquitectura validada
     * @return representación para la API
     */
    public static ArchitectureResponse from(final ArchitectureSpec spec) {
        return new ArchitectureResponse(spec.style(), spec.layout(), List.copyOf(spec.patterns()));
    }
}