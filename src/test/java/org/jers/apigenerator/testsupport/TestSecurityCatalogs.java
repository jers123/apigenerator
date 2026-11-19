package org.jers.apigenerator.testsupport;

import org.jers.apigenerator.core.catalog.Dependency;
import org.jers.apigenerator.core.catalog.DependencyKind;
import org.jers.apigenerator.core.catalog.GradleScope;
import org.jers.apigenerator.core.catalog.MavenScope;
import org.jers.apigenerator.core.security.SecurityCatalog;
import org.jers.apigenerator.core.security.SecurityMechanism;
import org.jers.apigenerator.core.security.SecurityRole;

import java.util.List;
import java.util.Set;

/** Catálogos de seguridad pequeños y predecibles para tests de cualquier capa. */
public final class TestSecurityCatalogs {

    private TestSecurityCatalogs() {
    }

    /**
     * HTTP Basic y OAuth2 Resource Server (con emisor) para la API, formulario
     * que choca con HTTP Basic, CORS y servidor de autorización (no reactivo).
     */
    public static SecurityCatalog small() {
        return new SecurityCatalog(List.of(
                mechanism("http-basic", "HTTP Basic", SecurityRole.API_AUTHENTICATION, false, Set.of()),
                mechanism("oauth2-resource-server", "OAuth2 Resource Server", SecurityRole.API_AUTHENTICATION, true,
                        Set.of()),
                mechanism("form-login", "Formulario", SecurityRole.BROWSER_LOGIN, false, Set.of("http-basic")),
                mechanism("cors", "CORS", SecurityRole.ADD_ON, false, Set.of()),
                mechanism("authorization-server", "Servidor propio", SecurityRole.AUTHORIZATION_SERVER, false,
                        Set.of())));
    }

    /** Método con el starter de seguridad; solo el servidor de autorización no funciona en reactivo. */
    public static SecurityMechanism mechanism(final String id, final String name, final SecurityRole role,
                                              final boolean requiresIssuer, final Set<String> conflicts) {
        return new SecurityMechanism(id, name, "Descripción de " + name, role, List.of(starter()),
                role != SecurityRole.AUTHORIZATION_SERVER, requiresIssuer, false, conflicts);
    }

    private static Dependency starter() {
        return new Dependency("Spring Security", "org.springframework.boot", "spring-boot-starter-security", null,
                DependencyKind.LIBRARY, MavenScope.COMPILE, GradleScope.IMPLEMENTATION, true, false);
    }
}