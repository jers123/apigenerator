package org.jers.apigenerator.web.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.jers.apigenerator.core.catalog.DependencyCategory;
import org.jers.apigenerator.core.catalog.DependencyGroup;

import java.util.List;

/**
 * Grupo tal como se guarda en el catálogo. Es el cuerpo de
 * {@code POST /api/v1/catalog/groups} y {@code PUT /api/v1/catalog/groups/{id}},
 * y la respuesta de {@code GET /api/v1/catalog/groups/{id}}.
 *
 * @param id                identificador estable; al editar debe coincidir con el de la URL
 * @param name              nombre visible; se puede cambiar
 * @param category          categoría del grupo
 * @param selectedByDefault si aparece marcado al abrir la pantalla de selección
 * @param requiresDatabase  si necesita una base de datos de desarrollo
 * @param dependencies      artefactos del grupo, en orden
 */
public record GroupDefinition(
        @NotBlank String id,
        @NotBlank String name,
        @NotNull DependencyCategory category,
        boolean selectedByDefault,
        boolean requiresDatabase,
        @NotEmpty List<@NotNull @Valid DependencyDefinition> dependencies) {

    /**
     * Convierte un grupo del núcleo.
     *
     * @param group grupo del catálogo
     * @return representación para la API
     */
    public static GroupDefinition from(final DependencyGroup group) {
        return new GroupDefinition(
                group.id(),
                group.name(),
                group.category(),
                group.selectedByDefault(),
                group.requiresDatabase(),
                group.dependencies().stream().map(DependencyDefinition::from).toList());
    }

    /**
     * Convierte la definición en un grupo del núcleo, que aplica sus propias
     * reglas.
     *
     * @return grupo validado
     * @throws org.jers.apigenerator.core.common.DomainValidationException si
     *         los datos no cumplen las reglas del grupo o de sus dependencias
     */
    public DependencyGroup toDomain() {
        return new DependencyGroup(id, name, category,
                dependencies.stream().map(DependencyDefinition::toDomain).toList(),
                selectedByDefault, requiresDatabase);
    }
}