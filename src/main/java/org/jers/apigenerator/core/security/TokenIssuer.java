package org.jers.apigenerator.core.security;

import org.jers.apigenerator.core.common.Preconditions;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Quién emite los tokens que usan la API o el inicio de sesión: el servidor
 * de autorización propio o un proveedor externo.
 */
public sealed interface TokenIssuer permits TokenIssuer.OwnAuthorizationServer, TokenIssuer.ExternalProvider {

    /**
     * Servidor de autorización propio, que se genera como un segundo proyecto.
     */
    record OwnAuthorizationServer() implements TokenIssuer {
    }

    /**
     * Proveedor de identidad externo del catálogo, con los datos que pide su URL de emisor.
     *
     * @param providerId id del proveedor en el catálogo
     * @param values     valor de cada parámetro del proveedor, por clave
     */
    record ExternalProvider(String providerId, Map<String, String> values) implements TokenIssuer {

        /**
         * Valida el id y protege los valores, descartando claves o valores nulos.
         *
         * @throws org.jers.apigenerator.core.common.DomainValidationException
         *         si el id no tiene formato de slug
         */
        public ExternalProvider {
            Preconditions.requireSlug(providerId, "providerId");
            values = values == null ? Map.of() : values.entrySet().stream()
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }
}