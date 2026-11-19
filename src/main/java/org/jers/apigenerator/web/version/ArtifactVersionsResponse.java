package org.jers.apigenerator.web.version;

import org.jers.apigenerator.core.version.ArtifactVersions;

import java.util.List;

/**
 * Versiones de un artefacto.
 *
 * @param groupId    groupId del artefacto
 * @param artifactId artifactId del artefacto
 * @param latest     versión estable más reciente, o {@code null} si no hay
 * @param versions   versiones estables, de la más nueva a la más antigua
 * @param available  {@code false} si no se pudo consultar el repositorio
 */
public record ArtifactVersionsResponse(
        String groupId,
        String artifactId,
        String latest,
        List<String> versions,
        boolean available) {

    /**
     * Convierte el resultado del núcleo a su representación JSON.
     *
     * @param result versiones consultadas
     * @return representación para la API
     */
    public static ArtifactVersionsResponse from(final ArtifactVersions result) {
        return new ArtifactVersionsResponse(
                result.coordinates().groupId(),
                result.coordinates().artifactId(),
                result.latest().orElse(null),
                result.versions(),
                result.available());
    }
}