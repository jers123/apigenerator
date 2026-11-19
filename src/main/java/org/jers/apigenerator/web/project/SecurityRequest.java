package org.jers.apigenerator.web.project;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.jers.apigenerator.core.security.SecuritySelection;

import java.util.Set;

/**
 * Seguridad elegida en el asistente.
 *
 * @param apiAuthenticationId id del método de autenticación de la API, u omitido
 * @param browserLoginId      id del método de inicio de sesión, u omitido
 * @param addOnIds            ids de los complementos; omitida equivale a ninguno
 * @param issuer              emisor de tokens, u omitido
 */
public record SecurityRequest(
        String apiAuthenticationId,
        String browserLoginId,
        Set<@NotBlank String> addOnIds,
        @Valid IssuerRequest issuer) {

    /**
     * Convierte la petición en la selección del núcleo.
     *
     * @return selección sin validar contra los catálogos
     */
    public SecuritySelection toDomain() {
        return new SecuritySelection(apiAuthenticationId, browserLoginId, addOnIds,
                issuer == null ? null : issuer.toDomain());
    }
}