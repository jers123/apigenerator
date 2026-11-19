package org.jers.apigenerator.core.security;

import org.jers.apigenerator.core.common.DomainValidationException;
import org.jers.apigenerator.core.common.Preconditions;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Proveedor de identidad externo que emite tokens: para que la API los valide
 * (Resource Server), para el inicio de sesión (OIDC Login) o para ambos.
 *
 * @param id                     identificador estable; también es el id de
 *                               registro en la configuración de Spring
 * @param name                   nombre visible
 * @param description            explicación breve, con advertencias de configuración
 * @param hosting                dónde funciona
 * @param issuerUriTemplate      URL del emisor con marcadores {@code {clave}},
 *                               o {@code null} si Spring Boot ya conoce al proveedor
 * @param parameters             datos que se piden para completar la plantilla
 * @param supportsResourceServer si la API puede validar sus tokens
 * @param supportsLogin          si sirve para el inicio de sesión
 * @param documentationUrl       documentación oficial, o {@code null}
 */
public record IdentityProvider(
        String id,
        String name,
        String description,
        ProviderHosting hosting,
        String issuerUriTemplate,
        List<ProviderParameter> parameters,
        boolean supportsResourceServer,
        boolean supportsLogin,
        String documentationUrl) {

    /**
     * Proveedores que Spring Boot configura sin URL de emisor
     * ({@code CommonOAuth2Provider}).
     */
    static final Set<String> SPRING_KNOWN = Set.of("google", "github", "facebook");

    /** Marcador de la plantilla: {@code {clave}}. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z][A-Za-z0-9]*)}");

    /**
     * Valida el proveedor y protege su lista de parámetros.
     *
     * @throws NullPointerException      si falta el alojamiento o algún parámetro es {@code null}
     * @throws DomainValidationException si algún dato no es válido o la
     *                                   plantilla no coincide con los parámetros
     */
    public IdentityProvider {
        Preconditions.requireSlug(id, "id");
        Preconditions.requireText(name, "name");
        description = description == null ? "" : description.strip();
        Objects.requireNonNull(hosting, "hosting es obligatorio");
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        if (!supportsResourceServer && !supportsLogin) {
            throw new DomainValidationException("El proveedor '" + name
                    + "' debe servir para la API, para el inicio de sesión o para ambos");
        }
        issuerUriTemplate = issuerUriTemplate == null || issuerUriTemplate.isBlank() ? null : issuerUriTemplate.strip();
        if (issuerUriTemplate == null) {
            requireKnownBySpring(id, name, supportsResourceServer, parameters);
        } else {
            requireMatchingParameters(name, issuerUriTemplate, parameters);
        }
        documentationUrl = documentationUrl == null || documentationUrl.isBlank() ? null : documentationUrl.strip();
        if (documentationUrl != null && !documentationUrl.startsWith("https://")) {
            throw new DomainValidationException("documentationUrl debe empezar por https://");
        }
    }

    /**
     * Arma la URL del emisor con los datos que escribió el usuario.
     *
     * @param values valor de cada parámetro, por clave
     * @return la URL final, o {@code null} si el proveedor no usa plantilla
     * @throws DomainValidationException si falta algún dato; el mensaje los nombra todos
     */
    public String issuerUri(final Map<String, String> values) {
        if (issuerUriTemplate == null) {
            return null;
        }
        List<String> missing = parameters.stream()
                .filter(parameter -> values.get(parameter.key()) == null || values.get(parameter.key()).isBlank())
                .map(ProviderParameter::label)
                .toList();
        if (!missing.isEmpty()) {
            throw new DomainValidationException("Faltan datos del proveedor " + name + ": " + missing);
        }
        Matcher matcher = PLACEHOLDER.matcher(issuerUriTemplate);
        return matcher.replaceAll(match -> Matcher.quoteReplacement(values.get(match.group(1)).strip()));
    }

    /**
     * Verifica que un proveedor sin plantilla lo conozca Spring Boot y solo sirva para el login.
     *
     * @param id                     id del proveedor
     * @param name                   nombre, para el mensaje
     * @param supportsResourceServer si dice servir para la API
     * @param parameters             parámetros declarados
     * @throws DomainValidationException si no se cumple alguna condición
     */
    private static void requireKnownBySpring(final String id, final String name, final boolean supportsResourceServer,
                                             final List<ProviderParameter> parameters) {
        if (supportsResourceServer) {
            throw new DomainValidationException("El proveedor '" + name
                    + "' necesita la URL del emisor para que la API pueda validar sus tokens");
        }
        if (!SPRING_KNOWN.contains(id)) {
            throw new DomainValidationException("El proveedor '" + name + "' necesita la URL del emisor; solo "
                    + SPRING_KNOWN.stream().sorted().toList() + " se configuran sin ella");
        }
        if (!parameters.isEmpty()) {
            throw new DomainValidationException("El proveedor '" + name + "' no tiene plantilla de emisor, así que "
                    + "no puede declarar parámetros");
        }
    }

    /**
     * Verifica que la plantilla sea https y que sus marcadores y los
     * parámetros coincidan uno a uno.
     *
     * @param name       nombre, para el mensaje
     * @param template   plantilla del emisor
     * @param parameters parámetros declarados
     * @throws DomainValidationException si no coinciden; el mensaje nombra la diferencia
     */
    private static void requireMatchingParameters(final String name, final String template,
                                                  final List<ProviderParameter> parameters) {
        if (!template.startsWith("https://") && !template.startsWith("{")) {
            throw new DomainValidationException("La URL del emisor de '" + name
                    + "' debe empezar por https:// o por un parámetro, como {baseUrl}");
        }
        Set<String> used = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(template);
        while (matcher.find()) {
            used.add(matcher.group(1));
        }
        Set<String> declared = new HashSet<>();
        for (ProviderParameter parameter : parameters) {
            if (!declared.add(parameter.key())) {
                throw new DomainValidationException("Parámetro repetido en '" + name + "': " + parameter.key());
            }
        }
        List<String> undeclared = used.stream().filter(key -> !declared.contains(key)).toList();
        List<String> unused = declared.stream().filter(key -> !used.contains(key)).sorted().toList();
        if (!undeclared.isEmpty()) {
            throw new DomainValidationException("La URL del emisor de '" + name + "' usa marcadores sin parámetro: "
                    + undeclared);
        }
        if (!unused.isEmpty()) {
            throw new DomainValidationException("Parámetros de '" + name + "' que la URL del emisor no usa: " + unused);
        }
    }
}