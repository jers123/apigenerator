package org.jers.apigenerator.core.catalog;

/**
 * Tipo de artefacto que representa una entrada del catálogo.
 *
 * <p>El tipo determina en qué parte del archivo de build se escribe el
 * artefacto. Solo las librerías ({@link #LIBRARY}) usan
 * {@link MavenScope} y {@link GradleScope}; el resto se ubican por su tipo.</p>
 */
public enum DependencyKind {

    /**
     * Librería normal: va en {@code <dependencies>} (Maven) o en el bloque
     * {@code dependencies} (Gradle).
     */
    LIBRARY,

    /**
     * Plugin de build: va en {@code <build><plugins>} (Maven) o en el bloque
     * {@code plugins} (Gradle).
     */
    PLUGIN,

    /**
     * Lista de versiones (BOM) importada en {@code <dependencyManagement>}
     * (Maven) o con {@code platform(...)} (Gradle).
     */
    BOM,

    /** POM padre del proyecto; solo aplica a Maven. */
    PARENT,

    /** Versión de la propia herramienta de build, usada por el wrapper. */
    BUILD_TOOL
}
