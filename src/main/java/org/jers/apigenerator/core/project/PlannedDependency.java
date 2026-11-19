package org.jers.apigenerator.core.project;

import org.jers.apigenerator.core.catalog.Dependency;
import org.jers.apigenerator.core.common.DomainValidationException;

import java.util.Objects;

/**
 * Dependencia del proyecto con su versión final.
 *
 * @param dependency artefacto del catálogo
 * @param version    versión que se escribirá en el build, o {@code null} si
 *                   la gestiona Spring Boot o un BOM
 */
public record PlannedDependency(Dependency dependency, String version) {

    /**
     * Valida la dependencia planificada.
     *
     * @throws NullPointerException      si falta el artefacto
     * @throws DomainValidationException si la versión está en blanco
     */
    public PlannedDependency {
        Objects.requireNonNull(dependency, "dependency es obligatoria");
        if (version != null && version.isBlank()) {
            throw new DomainValidationException("La versión de " + dependency.coordinates() + " está en blanco");
        }
    }

    /**
     * Indica si la versión no se escribe porque la gestiona otro artefacto.
     *
     * @return {@code true} si no tiene versión propia
     */
    public boolean managed() {
        return version == null;
    }
}