package org.jers.apigenerator.web.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jers.apigenerator.core.selection.BuildTool;
import org.jers.apigenerator.core.selection.SelectionRequest;

import java.util.Set;

/**
 * Cuerpo de {@code POST /api/v1/catalog/resolve}.
 *
 * @param groupIds  ids de los grupos marcados por el usuario
 * @param buildTool herramienta de build del proyecto
 * @param reactive  si el proyecto usa la pila reactiva
 */
public record ResolveRequest(
        @NotNull Set<@NotBlank String> groupIds,
        @NotNull BuildTool buildTool,
        boolean reactive) {

    /**
     * Convierte la petición HTTP en la petición del núcleo.
     *
     * @return petición de selección validada
     */
    public SelectionRequest toDomain() {
        return new SelectionRequest(groupIds, buildTool, reactive);
    }
}