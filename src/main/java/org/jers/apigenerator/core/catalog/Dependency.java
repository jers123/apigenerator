package org.jers.apigenerator.core.catalog;

import org.jers.apigenerator.core.common.DomainValidationException;
import org.jers.apigenerator.core.common.Preconditions;

import java.util.Objects;

/**
 * Artefacto del catálogo que el generador puede escribir en el archivo de
 * build de un proyecto.
 *
 * <p>Es inmutable y se valida al construirse: toda instancia que existe es
 * coherente. Solo las librerías ({@link DependencyKind#LIBRARY}) requieren
 * ámbitos; plugins, BOM, parent y herramienta de build se ubican por su
 * tipo.</p>
 *
 * @param name              nombre legible que se muestra en la interfaz
 * @param groupId           groupId de Maven del artefacto
 * @param artifactId        artifactId de Maven del artefacto
 * @param version           versión explícita, o {@code null} si la gestiona
 *                          el parent o un BOM
 * @param kind              tipo de artefacto; decide dónde se escribe en el build
 * @param mavenScope        ámbito en Maven; obligatorio solo para librerías
 * @param gradleScope       configuración en Gradle; obligatoria solo para librerías
 * @param includedByDefault {@code true} si se incluye siempre que su grupo
 *                          esté seleccionado
 * @param reactive          {@code true} si pertenece a la pila reactiva
 *                          (WebFlux, R2DBC)
 */
public record Dependency(
        String name,
        String groupId,
        String artifactId,
        String version,
        DependencyKind kind,
        MavenScope mavenScope,
        GradleScope gradleScope,
        boolean includedByDefault,
        boolean reactive) {

    /** groupId de los artefactos de Spring Boot, cuya versión fija el parent. */
    private static final String SPRING_BOOT_GROUP_ID = "org.springframework.boot";

    /**
     * Valida los datos del artefacto.
     *
     * @throws NullPointerException      si {@code kind} es {@code null}
     * @throws DomainValidationException si falta un texto obligatorio, la
     *                                  versión está en blanco, una librería
     *                                  no declara sus ámbitos o una
     *                                  dependencia reactiva se marca como
     *                                  incluida por defecto
     */
    public Dependency {
        Preconditions.requireText(name, "name");
        Preconditions.requireText(groupId, "groupId");
        Preconditions.requireText(artifactId, "artifactId");
        Objects.requireNonNull(kind, "kind es obligatorio");
        if (version != null && version.isBlank()) {
            throw new DomainValidationException(
                    "version no puede estar en blanco; use null si la gestiona el parent o un BOM");
        }
        if (kind == DependencyKind.LIBRARY && (mavenScope == null || gradleScope == null)) {
            throw new DomainValidationException(
                    "La librería '" + name + "' debe declarar mavenScope y gradleScope");
        }
        if (reactive && includedByDefault) {
            throw new DomainValidationException(
                    "La dependencia reactiva '" + name + "' no puede ser includedByDefault: "
                            + "solo se activa cuando el proyecto es reactivo");
        }
    }

    /**
     * Indica si el artefacto declara su propia versión.
     *
     * @return {@code true} si la versión debe escribirse en el archivo de build
     */
    public boolean hasExplicitVersion() {
        return version != null;
    }

    /**
     * Devuelve las coordenadas Maven sin versión.
     *
     * @return texto con el formato {@code groupId:artifactId}
     */
    public String coordinates() {
        return groupId + ":" + artifactId;
    }

    /**
     * Indica si la versión de este artefacto la impone el parent de Spring
     * Boot y, por tanto, el usuario no debe elegirla por separado.
     *
     * @return {@code true} para los artefactos de Spring Boot, excepto el
     *         propio parent
     */
    public boolean followsSpringBootVersion() {
        return SPRING_BOOT_GROUP_ID.equals(groupId) && kind != DependencyKind.PARENT;
    }
}