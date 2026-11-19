package org.jers.apigenerator.core.security;

import org.jers.apigenerator.core.catalog.Dependency;
import org.jers.apigenerator.core.common.DomainValidationException;
import org.jers.apigenerator.core.common.Preconditions;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Método de seguridad que el usuario puede elegir para su proyecto.
 *
 * @param id                identificador estable, con formato slug
 * @param name              nombre visible
 * @param description       explicación breve; vacía si no se indica, nunca {@code null}
 * @param role              para qué sirve
 * @param dependencies      artefactos que agrega al proyecto; nunca vacía
 * @param reactiveSupported si funciona en proyectos reactivos (WebFlux)
 * @param requiresIssuer    si necesita un emisor de tokens: servidor propio o
 *                          proveedor externo
 * @param managesUsers      si el proyecto debe guardar usuarios: en memoria o
 *                          en base de datos
 * @param conflictsWith     ids de los métodos con los que no se puede combinar
 */
public record SecurityMechanism(
        String id,
        String name,
        String description,
        SecurityRole role,
        List<Dependency> dependencies,
        boolean reactiveSupported,
        boolean requiresIssuer,
        boolean managesUsers,
        Set<String> conflictsWith) {

    /**
     * Valida el método y protege sus colecciones.
     *
     * @throws NullPointerException      si falta el tipo, la lista de
     *                                   dependencias o alguno de sus elementos
     * @throws DomainValidationException si el id o el nombre no son válidos,
     *                                   no tiene dependencias, pide emisor sin
     *                                   poder hacerlo o declara un choque
     *                                   consigo mismo
     */
    public SecurityMechanism {
        Preconditions.requireSlug(id, "id");
        Preconditions.requireText(name, "name");
        description = description == null ? "" : description.strip();
        Objects.requireNonNull(role, "role es obligatorio");
        Objects.requireNonNull(dependencies, "dependencies es obligatoria");
        if (dependencies.isEmpty()) {
            throw new DomainValidationException("El método de seguridad '" + name + "' no tiene dependencias");
        }
        dependencies = List.copyOf(dependencies);
        if (requiresIssuer && !role.mayRequireIssuer()) {
            throw new DomainValidationException("El método '" + name + "' es de tipo " + role.getLabel()
                    + ": solo la autenticación de la API y el inicio de sesión pueden necesitar emisor de tokens");
        }
        conflictsWith = conflictsWith == null ? Set.of() : Set.copyOf(conflictsWith);
        if (conflictsWith.contains(id)) {
            throw new DomainValidationException("El método '" + id + "' no puede chocar consigo mismo");
        }
    }
}