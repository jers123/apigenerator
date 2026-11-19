package org.jers.apigenerator.core.security;

import org.jers.apigenerator.core.catalog.Dependency;
import org.jers.apigenerator.core.common.DomainValidationException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Seguridad del proyecto ya validada contra los catálogos: los métodos
 * elegidos, el emisor de tokens y lo que hay que generar.
 *
 * @param apiAuthentication   autenticación de la API, o {@code null}
 * @param browserLogin        inicio de sesión en navegador, o {@code null}
 * @param addOns              complementos, en el orden del catálogo
 * @param authorizationServer servidor de autorización propio que se genera
 *                            como segundo proyecto, o {@code null}
 * @param provider            proveedor externo que emite los tokens, o {@code null}
 * @param issuerUri           URL del emisor; {@code null} si no hay emisor o
 *                            si Spring Boot ya conoce al proveedor
 */
public record ResolvedSecurity(
        SecurityMechanism apiAuthentication,
        SecurityMechanism browserLogin,
        List<SecurityMechanism> addOns,
        SecurityMechanism authorizationServer,
        IdentityProvider provider,
        String issuerUri) {

    /**
     * Protege la lista de complementos y verifica que haya un solo emisor.
     *
     * @throws NullPointerException      si la lista de complementos es {@code null}
     * @throws DomainValidationException si hay servidor propio y proveedor a la vez
     */
    public ResolvedSecurity {
        addOns = List.copyOf(Objects.requireNonNull(addOns, "addOns es obligatoria"));
        if (authorizationServer != null && provider != null) {
            throw new DomainValidationException("El emisor es el servidor propio o un proveedor, no ambos");
        }
    }

    /**
     * Proyecto sin seguridad.
     *
     * @return seguridad vacía
     */
    public static ResolvedSecurity none() {
        return new ResolvedSecurity(null, null, List.of(), null, null, null);
    }

    /**
     * Métodos que van en el proyecto principal: API, login y complementos, en ese orden.
     *
     * @return lista inmutable, posiblemente vacía
     */
    public List<SecurityMechanism> mechanisms() {
        List<SecurityMechanism> all = new ArrayList<>();
        if (apiAuthentication != null) {
            all.add(apiAuthentication);
        }
        if (browserLogin != null) {
            all.add(browserLogin);
        }
        all.addAll(addOns);
        return List.copyOf(all);
    }

    /**
     * Indica si el proyecto tiene alguna autenticación.
     *
     * @return {@code true} si hay autenticación de la API o inicio de sesión
     */
    public boolean enabled() {
        return apiAuthentication != null || browserLogin != null;
    }

    /**
     * Indica si el proyecto principal debe guardar usuarios.
     *
     * @return {@code true} si algún método elegido los gestiona
     */
    public boolean managesUsers() {
        return mechanisms().stream().anyMatch(SecurityMechanism::managesUsers);
    }

    /**
     * Indica si se genera el servidor de autorización como segundo proyecto.
     *
     * @return {@code true} si el emisor es el servidor propio
     */
    public boolean generatesAuthorizationServer() {
        return authorizationServer != null;
    }

    /**
     * Artefactos que la seguridad agrega al proyecto principal, sin repetir
     * coordenadas. El servidor propio no aporta aquí: sus artefactos van en el segundo proyecto.
     *
     * @return lista inmutable en el orden de {@link #mechanisms()}
     */
    public List<Dependency> dependencies() {
        Map<String, Dependency> unique = new LinkedHashMap<>();
        mechanisms().forEach(mechanism -> mechanism.dependencies()
                .forEach(dependency -> unique.putIfAbsent(dependency.coordinates(), dependency)));
        return List.copyOf(unique.values());
    }
}