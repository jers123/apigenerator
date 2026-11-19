package org.jers.apigenerator.web.catalog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jers.apigenerator.application.catalog.DependencyGroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * API REST para administrar los grupos del catálogo que no son de base de
 * datos.
 */
@RestController
@RequestMapping("/api/v1/catalog/groups")
@RequiredArgsConstructor
@Tag(name = "Grupos del catálogo", description = "Alta y edición de grupos de dependencias")
public class DependencyGroupController {

    /** Casos de uso de los grupos. */
    private final DependencyGroupService service;

    /**
     * Devuelve la definición de un grupo para editarla.
     *
     * @param id identificador del grupo
     * @return el grupo con todas sus dependencias
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtiene la definición de un grupo")
    public GroupDefinition findById(@PathVariable final String id) {
        return GroupDefinition.from(service.findById(id));
    }

    /**
     * Crea un grupo y responde con su ubicación.
     *
     * @param request definición del grupo
     * @return 201 con la cabecera {@code Location} y el grupo creado
     */
    @PostMapping
    @Operation(summary = "Crea un grupo (no de base de datos)")
    public ResponseEntity<GroupDefinition> create(@Valid @RequestBody final GroupDefinition request) {
        GroupDefinition created = GroupDefinition.from(service.create(request.toDomain()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Reemplaza la definición de un grupo existente.
     *
     * @param id      identificador del grupo, que no puede cambiar
     * @param request nueva definición
     * @return el grupo actualizado
     */
    @PutMapping("/{id}")
    @Operation(summary = "Edita un grupo (no de base de datos)")
    public GroupDefinition update(
            @PathVariable final String id,
            @Valid @RequestBody final GroupDefinition request) {
        return GroupDefinition.from(service.update(id, request.toDomain()));
    }
}