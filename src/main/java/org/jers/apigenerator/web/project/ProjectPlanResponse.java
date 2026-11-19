package org.jers.apigenerator.web.project;

import org.jers.apigenerator.core.project.Persistence;
import org.jers.apigenerator.core.project.ProjectPlan;

import java.util.List;

/**
 * Respuesta de {@code POST /api/v1/projects/plan}: el resumen de lo que se va a generar.
 *
 * @param groupId           groupId del proyecto
 * @param artifactId        artifactId del proyecto
 * @param basePackage       paquete raíz del código generado
 * @param springBootVersion versión de Spring Boot
 * @param architecture      arquitectura con los patrones nativos incluidos
 * @param options           Javadoc, pruebas y formato de configuración
 * @param persistence       dónde guarda los datos al ejecutarse
 * @param testPersistence   dónde guardan los datos las pruebas
 * @param dependencies      dependencias finales con su versión
 * @param database          base de desarrollo, o {@code null}
 * @param testDatabase      base de pruebas, o {@code null}
 * @param security          seguridad elegida y segundo proyecto
 */
public record ProjectPlanResponse(
        String groupId,
        String artifactId,
        String basePackage,
        String springBootVersion,
        ArchitectureResponse architecture,
        GenerationOptionsRequest options,
        Persistence persistence,
        Persistence testPersistence,
        List<PlannedDependencyResponse> dependencies,
        ConnectionResponse database,
        ConnectionResponse testDatabase,
        SecurityPlanResponse security) {

    /**
     * Convierte el plan del núcleo.
     *
     * @param plan plan del proyecto
     * @return representación para la API
     */
    public static ProjectPlanResponse from(final ProjectPlan plan) {
        return new ProjectPlanResponse(
                plan.spec().metadata().groupId(),
                plan.spec().metadata().artifactId(),
                plan.spec().metadata().basePackage(),
                plan.springBootVersion(),
                ArchitectureResponse.from(plan.spec().architecture()),
                GenerationOptionsRequest.from(plan.spec().options()),
                plan.spec().persistence(),
                plan.spec().testPersistence(),
                plan.dependencies().stream().map(PlannedDependencyResponse::from).toList(),
                ConnectionResponse.from(plan.spec().database()),
                ConnectionResponse.from(plan.spec().testDatabase()),
                SecurityPlanResponse.from(plan));
    }
}