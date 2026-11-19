package org.jers.apigenerator.web.version;

import jakarta.validation.constraints.NotBlank;
import org.jers.apigenerator.core.version.ArtifactCoordinates;

/**
 * Artefacto cuyas versiones se quieren consultar.
 *
 * @param groupId    groupId del artefacto
 * @param artifactId artifactId del artefacto
 */
public record ArtifactRequest(@NotBlank String groupId, @NotBlank String artifactId) {

    /**
     * Convierte la petición en coordenadas del núcleo.
     *
     * @return coordenadas validadas
     */
    public ArtifactCoordinates toDomain() {
        return new ArtifactCoordinates(groupId, artifactId);
    }
}