package org.jers.apigenerator.web.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jers.apigenerator.core.catalog.Dependency;
import org.jers.apigenerator.core.catalog.DependencyKind;
import org.jers.apigenerator.core.catalog.GradleScope;
import org.jers.apigenerator.core.catalog.MavenScope;

/**
 * Dependencia tal como se guarda en el catálogo, sin aplicar reglas de
 * selección. La usa el editor de grupos para leer y escribir.
 *
 * @param name              nombre legible
 * @param groupId           groupId de Maven
 * @param artifactId        artifactId de Maven
 * @param version           versión explícita, o {@code null} si la gestiona
 *                          Spring Boot o un BOM
 * @param kind              tipo de artefacto
 * @param mavenScope        ámbito en Maven; obligatorio solo para librerías
 * @param gradleScope       configuración en Gradle; obligatoria solo para librerías
 * @param includedByDefault si se incluye siempre que su grupo está activo
 * @param reactive          si pertenece a la pila reactiva
 */
public record DependencyDefinition(
        @NotBlank String name,
        @NotBlank String groupId,
        @NotBlank String artifactId,
        String version,
        @NotNull DependencyKind kind,
        MavenScope mavenScope,
        GradleScope gradleScope,
        boolean includedByDefault,
        boolean reactive) {

    /**
     * Convierte una dependencia del núcleo.
     *
     * @param dependency dependencia del catálogo
     * @return representación para la API
     */
    public static DependencyDefinition from(final Dependency dependency) {
        return new DependencyDefinition(
                dependency.name(),
                dependency.groupId(),
                dependency.artifactId(),
                dependency.version(),
                dependency.kind(),
                dependency.mavenScope(),
                dependency.gradleScope(),
                dependency.includedByDefault(),
                dependency.reactive());
    }

    /**
     * Convierte la definición en una dependencia del núcleo, que aplica sus
     * propias reglas.
     *
     * @return dependencia validada
     * @throws org.jers.apigenerator.core.common.DomainValidationException si
     *         los datos no cumplen las reglas de la dependencia
     */
    public Dependency toDomain() {
        return new Dependency(name, groupId, artifactId, version, kind, mavenScope, gradleScope,
                includedByDefault, reactive);
    }
}