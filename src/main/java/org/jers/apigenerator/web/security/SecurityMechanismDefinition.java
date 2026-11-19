package org.jers.apigenerator.web.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.jers.apigenerator.core.security.SecurityMechanism;
import org.jers.apigenerator.core.security.SecurityRole;
import org.jers.apigenerator.web.catalog.DependencyDefinition;

import java.util.List;
import java.util.Set;

/**
 * Método de seguridad tal como se guarda. Es el cuerpo de {@code POST} y
 * {@code PUT /api/v1/security/mechanisms} y la respuesta de sus {@code GET}.
 *
 * @param id                identificador estable; al editar debe coincidir con el de la URL
 * @param name              nombre visible
 * @param description       explicación breve
 * @param role              para qué sirve
 * @param reactiveSupported si funciona en proyectos reactivos
 * @param requiresIssuer    si necesita un emisor de tokens
 * @param managesUsers      si el proyecto debe guardar usuarios
 * @param conflictsWith     ids de los métodos con los que choca; omitida equivale a ninguno
 * @param dependencies      artefactos que agrega, en orden
 */
public record SecurityMechanismDefinition(
        @NotBlank String id,
        @NotBlank String name,
        String description,
        @NotNull SecurityRole role,
        boolean reactiveSupported,
        boolean requiresIssuer,
        boolean managesUsers,
        Set<@NotBlank String> conflictsWith,
        @NotEmpty List<@NotNull @Valid DependencyDefinition> dependencies) {

    /**
     * Convierte un método del núcleo.
     *
     * @param mechanism método del catálogo
     * @return representación para la API
     */
    public static SecurityMechanismDefinition from(final SecurityMechanism mechanism) {
        return new SecurityMechanismDefinition(
                mechanism.id(),
                mechanism.name(),
                mechanism.description(),
                mechanism.role(),
                mechanism.reactiveSupported(),
                mechanism.requiresIssuer(),
                mechanism.managesUsers(),
                mechanism.conflictsWith(),
                mechanism.dependencies().stream().map(DependencyDefinition::from).toList());
    }

    /**
     * Convierte la definición en un método del núcleo, que aplica sus propias reglas.
     *
     * @return método validado
     * @throws org.jers.apigenerator.core.common.DomainValidationException si
     *         los datos no cumplen las reglas del método o de sus dependencias
     */
    public SecurityMechanism toDomain() {
        return new SecurityMechanism(id, name, description, role,
                dependencies.stream().map(DependencyDefinition::toDomain).toList(),
                reactiveSupported, requiresIssuer, managesUsers, conflictsWith);
    }
}