package org.jers.apigenerator.web.catalog;

import org.jers.apigenerator.core.catalog.DependencyGroup;
import org.jers.apigenerator.core.selection.ResolvedSelection;

import java.util.List;

/**
 * Respuesta de {@code POST /api/v1/catalog/resolve}.
 *
 * @param groupIds     ds de los grupos que quedaron activos
 * @param dependencies dependencias finales del proyecto
 */
public record ResolveResponse(List<String> groupIds, List<DependencyResponse> dependencies) {

    /**
     * Convierte el resultado del núcleo a su representación JSON.
     *
     * @param selection resultado de aplicar las reglas
     * @return representación para la API
     */
    public static ResolveResponse from(final ResolvedSelection selection) {
        return new ResolveResponse(
                selection.groups().stream().map(DependencyGroup::id).toList(),
                selection.dependencies().stream().map(DependencyResponse::from).toList());
    }
}