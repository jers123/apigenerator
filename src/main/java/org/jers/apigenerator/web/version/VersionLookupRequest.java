package org.jers.apigenerator.web.version;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Cuerpo de {@code POST /api/v1/versions}.
 *
 * @param artifacts artefactos a consultar; máximo 100 por petición
 */
public record VersionLookupRequest(@NotNull @Size(max = 100) List<@Valid ArtifactRequest> artifacts) {
}