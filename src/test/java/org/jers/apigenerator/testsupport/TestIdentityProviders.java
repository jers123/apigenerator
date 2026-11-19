package org.jers.apigenerator.testsupport;

import org.jers.apigenerator.core.security.IdentityProvider;
import org.jers.apigenerator.core.security.IdentityProviderCatalog;
import org.jers.apigenerator.core.security.ProviderHosting;
import org.jers.apigenerator.core.security.ProviderParameter;

import java.util.List;

/** Proveedores de identidad pequeños y predecibles para tests de cualquier capa. */
public final class TestIdentityProviders {

    private TestIdentityProviders() {
    }

    /** Keycloak, con dos parámetros; sirve para la API y para el login. */
    public static IdentityProvider keycloak() {
        return new IdentityProvider("keycloak", "Keycloak", "Código abierto", ProviderHosting.SELF_HOSTED,
                "{baseUrl}/realms/{realm}",
                List.of(new ProviderParameter("baseUrl", "URL de Keycloak", "https://kc.example.com"),
                        new ProviderParameter("realm", "Realm", "myrealm")),
                true, true, "https://www.keycloak.org/documentation");
    }

    /** GitHub, sin plantilla; solo sirve para el login. */
    public static IdentityProvider github() {
        return new IdentityProvider("github", "GitHub", "OAuth2 sin OIDC", ProviderHosting.CLOUD,
                null, List.of(), false, true, null);
    }

    /** Catálogo con Keycloak y GitHub. */
    public static IdentityProviderCatalog small() {
        return new IdentityProviderCatalog(List.of(keycloak(), github()));
    }
}