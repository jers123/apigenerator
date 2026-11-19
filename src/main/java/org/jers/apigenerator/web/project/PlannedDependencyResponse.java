package org.jers.apigenerator.web.project;

import org.jers.apigenerator.core.catalog.DependencyKind;
import org.jers.apigenerator.core.catalog.GradleScope;
import org.jers.apigenerator.core.catalog.MavenScope;
import org.jers.apigenerator.core.project.PlannedDependency;

/**
 * Dependencia del plan con su versión final.
 *
 * @param name        nombre legible
 * @param groupId     groupId de Maven
 * @param artifactId  artifactId de Maven
 * @param kind        tipo de artefacto
 * @param mavenScope  ámbito en Maven, o {@code null} si no es librería
 * @param gradleScope configuración en Gradle, o {@code null} si no es librería
 * @param version     versión que se escribirá, o {@code null} si es gestionada
 * @param managed     si la versión la gestiona Spring Boot o un BOM
 */
public record PlannedDependencyResponse(
        String name,
        String groupId,
        String artifactId,
        DependencyKind kind,
        MavenScope mavenScope,
        GradleScope gradleScope,
        String version,
        boolean managed) {

    /**
     * Convierte una dependencia del plan.
     *
     * @param planned dependencia con su versión final
     * @return representación para la API
     */
    public static PlannedDependencyResponse from(final PlannedDependency planned) {
        return new PlannedDependencyResponse(
                planned.dependency().name(),
                planned.dependency().groupId(),
                planned.dependency().artifactId(),
                planned.dependency().kind(),
                planned.dependency().mavenScope(),
                planned.dependency().gradleScope(),
                planned.version(),
                planned.managed());
    }
}