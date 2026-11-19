package org.jers.apigenerator.web.project;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jers.apigenerator.application.project.ProjectService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST del proyecto a generar.
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Proyecto", description = "Plan del proyecto a generar")
public class ProjectController {

    /** Casos de uso del proyecto. */
    private final ProjectService service;

    /**
     * Valida lo que eligió el usuario y devuelve el plan del proyecto.
     *
     * @param request datos del proyecto
     * @return plan con las dependencias finales, sus versiones y las URL JDBC
     */
    @PostMapping("/plan")
    @Operation(summary = "Calcula el plan del proyecto sin generarlo")
    public ProjectPlanResponse plan(@Valid @RequestBody final ProjectRequest request) {
        return ProjectPlanResponse.from(service.plan(request.toDomain(service::engine)));
    }
}