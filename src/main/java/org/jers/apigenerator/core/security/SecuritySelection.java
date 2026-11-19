package org.jers.apigenerator.core.security;

import java.util.Set;

/**
 * Seguridad que el usuario eligió para su proyecto, tal como la pidió. Las
 * reglas que dependen de los catálogos las aplica {@link SecurityResolver}.
 *
 * @param apiAuthenticationId id del método de autenticación de la API, o
 *                            {@code null} si la API es pública
 * @param browserLoginId      id del método de inicio de sesión, o {@code null}
 * @param addOnIds            ids de los complementos; puede estar vacía
 * @param issuer              emisor de tokens, o {@code null} si ningún método lo usa
 */
public record SecuritySelection(
        String apiAuthenticationId,
        String browserLoginId,
        Set<String> addOnIds,
        TokenIssuer issuer) {

    /**
     * Normaliza los ids en blanco a {@code null} y protege los complementos.
     *
     * @throws NullPointerException si algún complemento es {@code null}
     */
    public SecuritySelection {
        apiAuthenticationId = blankToNull(apiAuthenticationId);
        browserLoginId = blankToNull(browserLoginId);
        addOnIds = addOnIds == null ? Set.of() : Set.copyOf(addOnIds);
    }

    /**
     * Proyecto sin seguridad: API pública, sin login ni complementos.
     *
     * @return selección vacía
     */
    public static SecuritySelection none() {
        return new SecuritySelection(null, null, Set.of(), null);
    }

    /**
     * Convierte un texto en blanco en {@code null}.
     *
     * @param value texto recibido
     * @return el texto sin espacios, o {@code null} si está vacío
     */
    private static String blankToNull(final String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}