package org.jers.apigenerator.core.selection;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jers.apigenerator.core.catalog.DependencyCategory;

import java.util.Arrays;

/**
 * Herramienta de build con la que se genera el proyecto.
 *
 * <p>Cada herramienta tiene una categoría de grupos exclusivos: los de su
 * propia categoría se incluyen siempre y los de la otra herramienta se
 * descartan.</p>
 */
@Getter
@RequiredArgsConstructor
public enum BuildTool {

    /** Apache Maven; genera un {@code pom.xml}. */
    MAVEN(DependencyCategory.MAVEN),

    /** Gradle; genera un {@code build.gradle}. */
    GRADLE(DependencyCategory.GRADLE);

    /** Categoría de los grupos exclusivos de esta herramienta. */
    private final DependencyCategory category;

    /**
     * Indica si los grupos de una categoría pertenecen a otra herramienta de
     * build y, por tanto, no aplican a esta.
     *
     * @param other categoría a evaluar
     * @return {@code true} si la categoría es exclusiva de otra herramienta
     */
    public boolean excludes(final DependencyCategory other) {
        return other != category
                && Arrays.stream(values()).anyMatch(tool -> tool.category == other);
    }
}