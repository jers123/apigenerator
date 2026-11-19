package org.jers.apigenerator.web.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jers.apigenerator.application.security.SecurityMechanismService;
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
import java.util.List;

/**
 * API REST para administrar los métodos de seguridad.
 */
@RestController
@RequestMapping("/api/v1/security/mechanisms")
@RequiredArgsConstructor
@Tag(name = "Seguridad", description = "Métodos de seguridad del proyecto generado")
public class SecurityMechanismController {

    /** Casos de uso de los métodos de seguridad. */
    private final SecurityMechanismService service;

    /**
     * Lista todos los métodos.
     *
     * @return métodos en el orden del catálogo
     */
    @GetMapping
    @Operation(summary = "Lista los métodos de seguridad")
    public List<SecurityMechanismDefinition> findAll() {
        return service.findAll().stream().map(SecurityMechanismDefinition::from).toList();
    }

    /**
     * Devuelve un método.
     *
     * @param id identificador del método
     * @return el método con sus dependencias
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un método de seguridad")
    public SecurityMechanismDefinition findById(@PathVariable final String id) {
        return SecurityMechanismDefinition.from(service.findById(id));
    }

    /**
     * Crea un método y responde con su ubicación.
     *
     * @param request definición del método
     * @return 201 con la cabecera {@code Location} y el método creado
     */
    @PostMapping
    @Operation(summary = "Crea un método de seguridad")
    public ResponseEntity<SecurityMechanismDefinition> create(
            @Valid @RequestBody final SecurityMechanismDefinition request) {
        SecurityMechanismDefinition created = SecurityMechanismDefinition.from(service.create(request.toDomain()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Reemplaza un método existente.
     *
     * @param id      identificador del método, que no puede cambiar
     * @param request nueva definición
     * @return el método actualizado
     */
    @PutMapping("/{id}")
    @Operation(summary = "Edita un método de seguridad")
    public SecurityMechanismDefinition update(
            @PathVariable final String id,
            @Valid @RequestBody final SecurityMechanismDefinition request) {
        return SecurityMechanismDefinition.from(service.update(id, request.toDomain()));
    }
}