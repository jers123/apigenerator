package org.jers.apigenerator.web.catalog;

import org.jers.apigenerator.core.catalog.Dependency;
import org.jers.apigenerator.core.catalog.DependencyKind;
import org.jers.apigenerator.core.catalog.GradleScope;
import org.jers.apigenerator.core.catalog.MavenScope;

/**
 * Representación JSON de una dependencia del catálogo.
 *
 * @param name              nombre legible
 * @param groupId           groupId de Maven
 * @param artifactId        artifactId de Maven
 * @param version           versión explícita, o {@code null} si la gestiona
 *                          el parent o un BOM
 * @param kind              tipo de artefacto
 * @param mavenScope        ámbito en Maven, o {@code null} si no es librería
 * @param gradleScope       configuración en Gradle, o {@code null} si no es
 *                          librería
 * @param includedByDefault si se incluye siempre que su grupo está activo
 * @param reactive          si pertenece a la pila reactiva
 * @param followsSpringBoot si su versión la fija el parent de Spring Boot
 */
public record DependencyResponse(
        String name,
        String groupId,
        String artifactId,
        String version,
        DependencyKind kind,
        MavenScope mavenScope,
        GradleScope gradleScope,
        boolean includedByDefault,
        boolean reactive,
        boolean followsSpringBoot) {

    /**
     * Convierte una dependencia del núcleo a su representación JSON.
     *
     * @param dependency dependencia del catálogo
     * @return representación para la API
     */
    public static DependencyResponse from(final Dependency dependency) {
        return new DependencyResponse(
                dependency.name(),
                dependency.groupId(),
                dependency.artifactId(),
                dependency.version(),
                dependency.kind(),
                dependency.mavenScope(),
                dependency.gradleScope(),
                dependency.includedByDefault(),
                dependency.reactive(),
                dependency.followsSpringBootVersion());
    }
}