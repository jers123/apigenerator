package org.jers.apigenerator.core.selection;

import java.util.Objects;
import java.util.Set;

/**
 * Elección del usuario sobre las dependencias del proyecto.
 *
 * @param groupIds   ids de los grupos marcados en la interfaz; inmutable
 * @param buildTool  herramienta de build del proyecto
 * @param reactive   {@code true} si el proyecto usa la pila reactiva
 */
public record SelectionRequest(Set<String> groupIds, BuildTool buildTool, boolean reactive) {

    /**
         * Valida la petición y protege el conjunto de nombres.
         *
     * @throws NullPointerException si el conjunto, alguno de sus nombres o la
     *                              herramienta de build es {@code null}
     */
    public SelectionRequest {
        Objects.requireNonNull(groupIds, "groupIds es obligatorio");
        Objects.requireNonNull(buildTool, "buildTool es obligatorio");
        groupIds = Set.copyOf(groupIds);
    }
}