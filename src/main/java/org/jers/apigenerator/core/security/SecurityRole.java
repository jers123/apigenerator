package org.jers.apigenerator.core.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Para qué sirve un método de seguridad; decide cómo se combina con los demás.
 */
@Getter
@RequiredArgsConstructor
public enum SecurityRole {

    /** Cómo demuestra quien llama a la API que tiene permiso; se elige uno o ninguno. */
    API_AUTHENTICATION("Autenticación de la API"),

    /** Inicio de sesión de personas desde el navegador; se elige uno o ninguno. */
    BROWSER_LOGIN("Inicio de sesión en navegador"),

    /** Complemento que se suma a la autenticación, como CORS o {@code @PreAuthorize}. */
    ADD_ON("Complemento"),

    /** Servidor propio que emite tokens; se genera como un segundo proyecto. */
    AUTHORIZATION_SERVER("Servidor de autorización");

    /** Nombre legible. */
    private final String label;

    /**
     * Indica si los métodos de este tipo pueden necesitar un emisor de tokens.
     *
     * @return {@code true} para la autenticación de la API y el inicio de sesión
     */
    public boolean mayRequireIssuer() {
        return this == API_AUTHENTICATION || this == BROWSER_LOGIN;
    }
}