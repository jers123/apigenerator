package org.jers.apigenerator.web.project;

import jakarta.validation.constraints.NotNull;
import org.jers.apigenerator.core.architecture.ArchitectureSpec;
import org.jers.apigenerator.core.architecture.ArchitectureStyle;
import org.jers.apigenerator.core.architecture.DesignPattern;
import org.jers.apigenerator.core.architecture.ModuleLayout;

import java.util.Set;

/**
 * Arquitectura en la petición.
 *
 * @param style    estilo de arquitectura
 * @param layout   reparto en módulos
 * @param patterns patrones elegidos; los nativos se agregan solos. Omitido
 *                 equivale a ninguno
 */
public record ArchitectureRequest(
        @NotNull ArchitectureStyle style,
        @NotNull ModuleLayout layout,
        Set<@NotNull DesignPattern> patterns) {

    /**
     * Convierte la petición en la arquitectura del núcleo.
     *
     * @return arquitectura validada, con los patrones nativos incluidos
     * @throws org.jers.apigenerator.core.common.DomainValidationException si
     *         la combinación no es válida
     */
    public ArchitectureSpec toDomain() {
        return new ArchitectureSpec(style, layout, patterns == null ? Set.of() : patterns);
    }
}