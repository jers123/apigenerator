package org.jers.apigenerator.core.selection;

import org.jers.apigenerator.core.catalog.Dependency;
import org.jers.apigenerator.core.catalog.DependencyGroup;

import java.util.List;
import java.util.Objects;

/**
 * Resultado de aplicar las reglas de selección.
 *
 * @param groups       grupos que quedaron activos, en el orden del catálogo
 * @param dependencies dependencias finales que se escribirán en el build, en
 *                     el orden del catálogo
 */
public record ResolvedSelection(List<DependencyGroup> groups, List<Dependency> dependencies) {

    /**
     * Protege las listas del resultado.
     *
     * @throws NullPointerException si alguna lista o elemento es {@code null}
     */
    public ResolvedSelection {
        Objects.requireNonNull(groups, "groupIds es obligatoria");
        Objects.requireNonNull(dependencies, "dependencies es obligatoria");
        groups = List.copyOf(groups);
        dependencies = List.copyOf(dependencies);
    }
}