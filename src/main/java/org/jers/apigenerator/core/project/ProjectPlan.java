package org.jers.apigenerator.core.project;

import org.jers.apigenerator.core.common.Preconditions;
import org.jers.apigenerator.core.security.ResolvedSecurity;

import java.util.List;
import java.util.Objects;

/**
 * Todo lo necesario para generar un proyecto: lo que pidió el usuario, las
 * dependencias finales con sus versiones y la seguridad validada.
 *
 * @param spec              proyecto pedido por el usuario
 * @param springBootVersion versión de Spring Boot, tomada del parent
 * @param dependencies      dependencias finales con su versión: las del
 *                          catálogo en su orden y, al final, las de seguridad
 * @param security          seguridad validada contra los catálogos
 */
public record ProjectPlan(
        ProjectSpec spec,
        String springBootVersion,
        List<PlannedDependency> dependencies,
        ResolvedSecurity security) {

    /** Sufijo del artifactId del servidor de autorización generado. */
    static final String AUTHORIZATION_SERVER_SUFFIX = "-auth-server";

    /**
     * Valida el plan y protege su lista.
     *
     * @throws NullPointerException si falta el proyecto, la lista o la seguridad
     * @throws org.jers.apigenerator.core.common.DomainValidationException si
     *         falta la versión de Spring Boot
     */
    public ProjectPlan {
        Objects.requireNonNull(spec, "spec es obligatorio");
        Preconditions.requireText(springBootVersion, "springBootVersion");
        dependencies = List.copyOf(Objects.requireNonNull(dependencies, "dependencies es obligatoria"));
        Objects.requireNonNull(security, "security es obligatoria");
    }

    /**
     * ArtifactId del segundo proyecto, el servidor de autorización.
     *
     * @return el artifactId del proyecto con {@value #AUTHORIZATION_SERVER_SUFFIX},
     *         o {@code null} si no se genera
     */
    public String authorizationServerArtifactId() {
        return security.generatesAuthorizationServer()
                ? spec.metadata().artifactId() + AUTHORIZATION_SERVER_SUFFIX
                : null;
    }
}