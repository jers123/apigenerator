package org.jers.apigenerator.core.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Dónde funciona un proveedor de identidad.
 */
@Getter
@RequiredArgsConstructor
public enum ProviderHosting {

    /** Servicio en la nube del fabricante; solo se configura. */
    CLOUD("En la nube"),

    /** Software que el usuario instala y opera, como Keycloak. */
    SELF_HOSTED("Lo instalas tú");

    /** Nombre legible. */
    private final String label;
}