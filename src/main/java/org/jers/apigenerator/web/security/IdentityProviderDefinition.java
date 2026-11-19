package org.jers.apigenerator.web.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jers.apigenerator.core.security.IdentityProvider;
import org.jers.apigenerator.core.security.ProviderHosting;
import org.jers.apigenerator.core.security.ProviderParameter;

import java.util.List;

/**
 * Proveedor de identidad tal como se guarda. Es el cuerpo de {@code POST} y
 * {@code PUT /api/v1/security/identity-providers} y la respuesta de sus {@code GET}.
 *
 * @param id                     identificador estable; al editar debe coincidir con el de la URL
 * @param name                   nombre visible
 * @param description            explicación breve
 * @param hosting                dónde funciona
 * @param issuerUriTemplate      URL del emisor con marcadores {@code {clave}}, o {@code null}
 * @param parameters             datos que se piden; omitida equivale a ninguno
 * @param supportsResourceServer si la API puede validar sus tokens
 * @param supportsLogin          si sirve para el inicio de sesión
 * @param documentationUrl       documentación oficial, o {@code null}
 */
public record IdentityProviderDefinition(
        @NotBlank String id,
        @NotBlank String name,
        String description,
        @NotNull ProviderHosting hosting,
        String issuerUriTemplate,
        List<@NotNull @Valid Parameter> parameters,
        boolean supportsResourceServer,
        boolean supportsLogin,
        String documentationUrl) {

    /**
     * Convierte un proveedor del núcleo.
     *
     * @param provider proveedor del catálogo
     * @return representación para la API
     */
    public static IdentityProviderDefinition from(final IdentityProvider provider) {
        return new IdentityProviderDefinition(
                provider.id(),
                provider.name(),
                provider.description(),
                provider.hosting(),
                provider.issuerUriTemplate(),
                provider.parameters().stream().map(Parameter::from).toList(),
                provider.supportsResourceServer(),
                provider.supportsLogin(),
                provider.documentationUrl());
    }

    /**
     * Convierte la definición en un proveedor del núcleo, que aplica sus propias reglas.
     *
     * @return proveedor validado
     * @throws org.jers.apigenerator.core.common.DomainValidationException si
     *         los datos no cumplen las reglas del proveedor
     */
    public IdentityProvider toDomain() {
        return new IdentityProvider(id, name, description, hosting, issuerUriTemplate,
                parameters == null ? List.of() : parameters.stream().map(Parameter::toDomain).toList(),
                supportsResourceServer, supportsLogin, documentationUrl);
    }

    /**
     * Dato que se pide para completar la URL del emisor.
     *
     * @param key     nombre del marcador, sin llaves
     * @param label   texto que ve el usuario
     * @param example valor de ejemplo
     */
    public record Parameter(@NotBlank String key, @NotBlank String label, String example) {

        /**
         * Convierte un parámetro del núcleo.
         *
         * @param parameter parámetro
         * @return representación para la API
         */
        static Parameter from(final ProviderParameter parameter) {
            return new Parameter(parameter.key(), parameter.label(), parameter.example());
        }

        /**
         * Convierte el parámetro al núcleo.
         *
         * @return parámetro validado
         */
        ProviderParameter toDomain() {
            return new ProviderParameter(key, label, example);
        }
    }
}