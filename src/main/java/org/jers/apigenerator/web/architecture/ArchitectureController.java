package org.jers.apigenerator.web.architecture;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST del catálogo de arquitecturas y patrones.
 *
 * <p>El catálogo es fijo: cada estilo y patrón necesita soporte del
 * generador, así que no se edita como el catálogo de dependencias.</p>
 */
@RestController
@RequestMapping("/api/v1/architectures")
@Tag(name = "Arquitecturas", description = "Estilos, repartos en módulos y patrones de diseño")
public class ArchitectureController {

    /**
     * Devuelve los estilos, repartos y patrones con su compatibilidad.
     *
     * @return catálogo completo
     */
    @GetMapping
    @Operation(summary = "Lista los estilos de arquitectura y la compatibilidad de cada patrón")
    public ArchitectureCatalogResponse catalog() {
        return ArchitectureCatalogResponse.current();
    }
}