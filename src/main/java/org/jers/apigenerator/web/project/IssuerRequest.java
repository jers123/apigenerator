package org.jers.apigenerator.web.project;

import jakarta.validation.constraints.NotNull;
import org.jers.apigenerator.core.common.DomainValidationException;
import org.jers.apigenerator.core.security.TokenIssuer;

import java.util.Map;

/**
 * Emisor de tokens elegido: el servidor propio o un proveedor con sus datos.
 *
 * @param type       tipo de emisor
 * @param providerId id del proveedor; solo con {@link Type#PROVIDER}
 * @param values     datos del proveedor por clave; solo con {@link Type#PROVIDER}
 */
public record IssuerRequest(@NotNull Type type, String providerId, Map<String, String> values) {

    /**
     * Tipo de emisor.
     */
    public enum Type {

        /** Servidor de autorización propio, generado como segundo proyecto. */
        OWN_SERVER,

        /** Proveedor de identidad externo del catálogo. */
        PROVIDER
    }

    /**
     * Convierte la petición en el emisor del núcleo.
     *
     * @return emisor validado
     * @throws DomainValidationException si el servidor propio trae datos de
     *                                   proveedor o el id del proveedor no es válido
     */
    public TokenIssuer toDomain() {
        return switch (type) {
            case OWN_SERVER -> {
                if (providerId != null || (values != null && !values.isEmpty())) {
                    throw new DomainValidationException("El servidor propio no lleva proveedor ni datos de proveedor");
                }
                yield new TokenIssuer.OwnAuthorizationServer();
            }
            case PROVIDER -> new TokenIssuer.ExternalProvider(providerId, values);
        };
    }

    /**
     * Tipo de emisor de una seguridad resuelta, para la respuesta.
     *
     * @param ownServer   si el emisor es el servidor propio
     * @param hasProvider si el emisor es un proveedor
     * @return el tipo, o {@code null} si no hay emisor
     */
    static Type of(final boolean ownServer, final boolean hasProvider) {
        if (ownServer) {
            return Type.OWN_SERVER;
        }
        return hasProvider ? Type.PROVIDER : null;
    }
}