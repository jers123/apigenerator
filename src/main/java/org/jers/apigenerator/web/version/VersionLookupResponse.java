package org.jers.apigenerator.web.version;

import java.util.List;

/**
 * Respuesta de {@code POST /api/v1/versions}.
 *
 * @param artifacts un resultado por artefacto consultado
 */
public record VersionLookupResponse(List<ArtifactVersionsResponse> artifacts) {
}