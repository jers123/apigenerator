package org.jers.apigenerator.web.catalog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jers.apigenerator.application.catalog.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST del catálogo de dependencias.
 */
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
@Tag(name = "Catálogo", description = "Dependencias disponibles y reglas de selección")
public class CatalogController {

    /** Casos de uso del catálogo. */
    private final CatalogService catalogService;

    /**
     * Devuelve el catálogo completo para pintar la pantalla de selección.
     *
     * @return grupos con sus dependencias
     */
    @GetMapping
    @Operation(summary = "Lista los grupos de dependencias disponibles")
    public CatalogResponse getCatalog() {
        return CatalogResponse.from(catalogService.catalog());
    }

    /**
     * Aplica las reglas de selección a lo que marcó el usuario.
     *
     * @param request grupos marcados, herramienta de build y modo reactivo
     * @return grupos activos y dependencias finales
     */
    @PostMapping("/resolve")
    @Operation(summary = "Aplica las reglas de selección y devuelve las dependencias finales")
    public ResolveResponse resolve(@Valid @RequestBody final ResolveRequest request) {
        return ResolveResponse.from(catalogService.resolve(request.toDomain()));
    }
}