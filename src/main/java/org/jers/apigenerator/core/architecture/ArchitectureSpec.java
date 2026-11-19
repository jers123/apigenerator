package org.jers.apigenerator.core.architecture;

import org.jers.apigenerator.core.common.DomainValidationException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Arquitectura elegida para el proyecto generado.
 *
 * <p>Los patrones nativos del estilo se agregan siempre, aunque el usuario
 * no los marque. Los contraindicados y los que no aplican se rechazan con
 * su motivo.</p>
 *
 * @param style    estilo de arquitectura
 * @param layout   reparto en uno o varios módulos de build
 * @param patterns patrones del proyecto: los nativos más los elegidos, en
 *                 el orden del catálogo; nunca modificable
 */
public record ArchitectureSpec(ArchitectureStyle style, ModuleLayout layout, Set<DesignPattern> patterns) {

    /**
     * Valida la combinación y completa los patrones nativos.
     *
     * @throws NullPointerException      si falta el estilo, el reparto o el conjunto
     * @throws DomainValidationException si el estilo no admite varios módulos o
     *                                   algún patrón no se puede usar con él
     */
    public ArchitectureSpec {
        Objects.requireNonNull(style, "style es obligatorio");
        Objects.requireNonNull(layout, "layout es obligatorio");
        Objects.requireNonNull(patterns, "patterns es obligatorio");
        if (layout == ModuleLayout.MULTI_MODULE && !style.isMultiModule()) {
            throw new DomainValidationException("El estilo " + style.getLabel()
                    + " no se puede generar en varios módulos: no tiene capas que separar");
        }
        List<String> rejected = patterns.stream()
                .sorted()
                .filter(pattern -> !pattern.compatibilityWith(style).selectable())
                .map(pattern -> pattern.getLabel() + ": " + pattern.getReason())
                .toList();
        if (!rejected.isEmpty()) {
            throw new DomainValidationException("Patrones que no se pueden usar con " + style.getLabel() + " — "
                    + String.join("; ", rejected));
        }
        Set<DesignPattern> all = DesignPattern.nativeTo(style);
        all.addAll(patterns);
        patterns = Collections.unmodifiableSet(all);
    }
}