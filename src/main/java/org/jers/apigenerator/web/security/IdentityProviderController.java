package org.jers.apigenerator.web.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jers.apigenerator.application.security.IdentityProviderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * API REST para administrar los proveedores de identidad externos.
 */
@RestController
@RequestMapping("/api/v1/security/identity-providers")
@RequiredArgsConstructor
@Tag(name = "Proveedores de identidad", description = "Emisores de tokens externos para la API y el login")
public class IdentityProviderController {

    /** Casos de uso de los proveedores. */
    private final IdentityProviderService service;

    /**
     * Lista todos los proveedores.
     *
     * @return proveedores en el orden del catálogo
     */
    @GetMapping
    @Operation(summary = "Lista los proveedores de identidad")
    public List<IdentityProviderDefinition> findAll() {
        return service.findAll().stream().map(IdentityProviderDefinition::from).toList();
    }

    /**
     * Devuelve un proveedor.
     *
     * @param id identificador del proveedor
     * @return el proveedor
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un proveedor de identidad")
    public IdentityProviderDefinition findById(@PathVariable final String id) {
        return IdentityProviderDefinition.from(service.findById(id));
    }

    /**
     * Crea un proveedor y responde con su ubicación.
     *
     * @param request definición del proveedor
     * @return 201 con la cabecera {@code Location} y el proveedor creado
     */
    @PostMapping
    @Operation(summary = "Crea un proveedor de identidad")
    public ResponseEntity<IdentityProviderDefinition> create(
            @Valid @RequestBody final IdentityProviderDefinition request) {
        IdentityProviderDefinition created = IdentityProviderDefinition.from(service.create(request.toDomain()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Reemplaza un proveedor existente.
     *
     * @param id      identificador del proveedor, que no puede cambiar
     * @param request nueva definición
     * @return el proveedor actualizado
     */
    @PutMapping("/{id}")
    @Operation(summary = "Edita un proveedor de identidad")
    public IdentityProviderDefinition update(
            @PathVariable final String id,
            @Valid @RequestBody final IdentityProviderDefinition request) {
        return IdentityProviderDefinition.from(service.update(id, request.toDomain()));
    }
}