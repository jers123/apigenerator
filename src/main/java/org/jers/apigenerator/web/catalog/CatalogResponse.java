package org.jers.apigenerator.web.catalog;

import org.jers.apigenerator.core.catalog.DependencyCatalog;

import java.util.List;

/**
 * Respuesta de {@code GET /api/v1/catalog}.
 *
 * @param groups grupos del catálogo, en el orden en que deben mostrarse
 */
public record CatalogResponse(List<GroupResponse> groups) {

    /**
     * Convierte el catálogo del núcleo a su representación JSON.
     *
     * @param catalog catálogo vigente
     * @return representación para la API
     */
    public static CatalogResponse from(final DependencyCatalog catalog) {
        return new CatalogResponse(catalog.groups().stream().map(GroupResponse::from).toList());
    }
}