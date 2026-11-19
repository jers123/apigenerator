package org.jers.apigenerator.web.version;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jers.apigenerator.application.version.VersionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/versions")
@RequiredArgsConstructor
@Tag(name = "Versiones", description = "Versiones estables publicadas en Maven Central")
public class VersionController {

    /** Caso de uso de consulta de versiones. */
    private final VersionService versionService;

    /**
     * Consulta las versiones de varios artefactos a la vez.
     *
     * @param request artefactos a consultar
     * @return versiones de cada artefacto
     */
    @PostMapping
    @Operation(summary = "Consulta las versiones estables de varios artefactos")
    public VersionLookupResponse lookup(@Valid @RequestBody final VersionLookupRequest request) {
        return new VersionLookupResponse(versionService
                .lookup(request.artifacts().stream().map(ArtifactRequest::toDomain).toList())
                .stream()
                .map(ArtifactVersionsResponse::from)
                .toList());
    }
}