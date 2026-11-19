package org.jers.apigenerator.web.database;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jers.apigenerator.application.database.DatabaseEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * API REST para administrar los motores de base de datos.
 */
@RestController
@RequestMapping("/api/v1/databases")
@RequiredArgsConstructor
@Tag(name = "Bases de datos", description = "Motores disponibles para el proyecto generado")
public class DatabaseEngineController {

    /** Casos de uso de los motores. */
    private final DatabaseEngineService service;

    /**
     * Lista todos los motores.
     *
     * @return motores en orden de creación
     */
    @GetMapping
    @Operation(summary = "Lista los motores de base de datos")
    public List<DatabaseEngineResponse> findAll() {
        return service.findAll().stream().map(DatabaseEngineResponse::from).toList();
    }

    /**
     * Devuelve un motor.
     *
     * @param id identificador del motor
     * @return el motor
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un motor de base de datos")
    public DatabaseEngineResponse findById(@PathVariable final String id) {
        return DatabaseEngineResponse.from(service.findById(id));
    }

    /**
     * Crea un motor y responde con su ubicación.
     *
     * @param request datos del motor
     * @return 201 con la cabecera {@code Location} y el motor creado
     */
    @PostMapping
    @Operation(summary = "Crea un motor de base de datos")
    public ResponseEntity<DatabaseEngineResponse> create(@Valid @RequestBody final DatabaseEngineRequest request) {
        DatabaseEngineResponse created = DatabaseEngineResponse.from(service.create(request.toDomain()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Reemplaza los datos de un motor existente.
     *
     * @param id      identificador del motor, que no puede cambiar
     * @param request nuevos datos
     * @return el motor actualizado
     */
    @PutMapping("/{id}")
    @Operation(summary = "Edita un motor de base de datos")
    public DatabaseEngineResponse update(
            @PathVariable final String id,
            @Valid @RequestBody final DatabaseEngineRequest request) {
        return DatabaseEngineResponse.from(service.update(id, request.toDomain()));
    }
}