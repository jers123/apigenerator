package org.jers.apigenerator.web.catalog;

import org.jers.apigenerator.core.catalog.Dependency;
import org.jers.apigenerator.core.catalog.DependencyCategory;
import org.jers.apigenerator.core.catalog.DependencyGroup;
import org.jers.apigenerator.core.selection.DependencySelector;

import java.util.List;

/**
 * Representación JSON de un grupo de dependencias.
 *
 * @param id                   identificador estable del grupo)
 * @param name                 nombre del grupo; lo identifica en la API
 * @param category             categoría del grupo
 * @param categoryLabel        nombre legible de la categoría
 * @param selectedByDefault    si el grupo aparece marcado al abrir la pantalla
 * @param requiresDatabase     si necesita una base de datos de desarrollo
 * @param standardDependencies dependencias que aporta en un proyecto no reactivo
 * @param reactiveDependencies dependencias que aporta en un proyecto reactivo
 */
public record GroupResponse(
        String id,
        String name,
        DependencyCategory category,
        String categoryLabel,
        boolean selectedByDefault,
        boolean requiresDatabase,
        List<DependencyResponse> standardDependencies,
        List<DependencyResponse> reactiveDependencies) {

    /**
     * Convierte un grupo del núcleo a su representación JSON, aplicando las
     * reglas de selección para cada modo.
     *
     * @param group grupo del catálogo
     * @return representación para la API
     */
    public static GroupResponse from(final DependencyGroup group) {
        return new GroupResponse(
                group.id(),
                group.name(),
                group.category(),
                group.category().getLabel(),
                group.selectedByDefault(),
                group.requiresDatabase(),
                toResponses(DependencySelector.applicableDependencies(group, false)),
                toResponses(DependencySelector.applicableDependencies(group, true)));
    }

    /**
     * Convierte una lista de dependencias del núcleo.
     *
     * @param dependencies dependencias del núcleo
     * @return representaciones JSON
     */
    private static List<DependencyResponse> toResponses(final List<Dependency> dependencies) {
        return dependencies.stream().map(DependencyResponse::from).toList();
    }
}