package org.jers.apigenerator.core.security;

import org.jers.apigenerator.core.common.DomainValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Valida la seguridad elegida contra el catálogo de métodos y el de
 * proveedores, y la convierte en {@link ResolvedSecurity}.
 *
 * <ul>
 *   <li>Cada id debe existir y ser del tipo del campo en que se eligió.</li>
 *   <li>Los complementos necesitan autenticación de la API.</li>
 *   <li>Los métodos elegidos no pueden chocar entre sí.</li>
 *   <li>En un proyecto reactivo todo método elegido, y el servidor propio,
 *       debe funcionar en reactivo.</li>
 *   <li>El emisor es obligatorio si algún método lo necesita y se rechaza
 *       si ninguno lo usa.</li>
 *   <li>Un proveedor debe servir para lo que se usa (API o login) y recibir
 *       exactamente sus datos.</li>
 * </ul>
 *
 * <p>No tiene estado mutable: una misma instancia puede atender peticiones concurrentes.</p>
 */
public final class SecurityResolver {

    /** Puerto del servidor de autorización generado. */
    public static final int AUTHORIZATION_SERVER_PORT = 9000;

    /** URL del emisor cuando es el servidor propio, en desarrollo. */
    public static final String OWN_SERVER_ISSUER = "http://localhost:" + AUTHORIZATION_SERVER_PORT;

    /** Métodos de seguridad disponibles. */
    private final SecurityCatalog mechanisms;

    /** Proveedores de identidad disponibles. */
    private final IdentityProviderCatalog providers;

    /**
     * Crea el validador para unos catálogos.
     *
     * @param mechanisms catálogo de métodos de seguridad
     * @param providers  catálogo de proveedores de identidad
     * @throws NullPointerException si algún catálogo es {@code null}
     */
    public SecurityResolver(final SecurityCatalog mechanisms, final IdentityProviderCatalog providers) {
        this.mechanisms = Objects.requireNonNull(mechanisms, "mechanisms es obligatorio");
        this.providers = Objects.requireNonNull(providers, "providers es obligatorio");
    }

    /**
     * Valida la selección y la resuelve contra los catálogos.
     *
     * @param selection seguridad pedida por el usuario
     * @param reactive  si el proyecto es reactivo
     * @return seguridad resuelta
     * @throws DomainValidationException si no se cumple alguna regla; el
     *                                   mensaje nombra los métodos o datos implicados
     */
    public ResolvedSecurity resolve(final SecuritySelection selection, final boolean reactive) {
        Objects.requireNonNull(selection, "selection es obligatoria");
        SecurityMechanism api = chosen(selection.apiAuthenticationId(), SecurityRole.API_AUTHENTICATION);
        SecurityMechanism login = chosen(selection.browserLoginId(), SecurityRole.BROWSER_LOGIN);
        List<SecurityMechanism> addOns = addOns(selection.addOnIds());
        if (!addOns.isEmpty() && api == null) {
            throw new DomainValidationException("Los complementos " + names(addOns)
                    + " necesitan autenticación de la API; elija una o quítelos");
        }
        List<SecurityMechanism> chosen = new ArrayList<>(Stream.of(api, login).filter(Objects::nonNull).toList());
        chosen.addAll(addOns);
        rejectConflicts(chosen);
        rejectNonReactive(chosen, reactive);

        List<SecurityMechanism> needingIssuer = chosen.stream().filter(SecurityMechanism::requiresIssuer).toList();
        TokenIssuer issuer = selection.issuer();
        if (needingIssuer.isEmpty()) {
            if (issuer != null) {
                throw new DomainValidationException("Ningún método elegido usa emisor de tokens; quite el emisor");
            }
            return new ResolvedSecurity(api, login, addOns, null, null, null);
        }
        if (issuer == null) {
            throw new DomainValidationException("Estos métodos necesitan un emisor de tokens: "
                    + names(needingIssuer) + "; elija el servidor propio o un proveedor");
        }
        if (issuer instanceof TokenIssuer.ExternalProvider external) {
            IdentityProvider provider = provider(external, api, login);
            return new ResolvedSecurity(api, login, addOns, null, provider, provider.issuerUri(external.values()));
        }
        return new ResolvedSecurity(api, login, addOns, ownServer(reactive), null, OWN_SERVER_ISSUER);
    }

    /**
     * Busca un método elegido y verifica su tipo.
     *
     * @param id   id elegido, o {@code null}
     * @param role tipo que corresponde al campo
     * @return el método, o {@code null} si no se eligió
     * @throws DomainValidationException si no existe o es de otro tipo
     */
    private SecurityMechanism chosen(final String id, final SecurityRole role) {
        if (id == null) {
            return null;
        }
        SecurityMechanism mechanism = mechanisms.findMechanism(id)
                .orElseThrow(() -> new DomainValidationException("No existe el método de seguridad '" + id + "'"));
        if (mechanism.role() != role) {
            throw new DomainValidationException("'" + mechanism.name() + "' es de tipo " + mechanism.role().getLabel()
                    + ", no de tipo " + role.getLabel());
        }
        return mechanism;
    }

    /**
     * Busca los complementos elegidos.
     *
     * @param ids ids elegidos
     * @return complementos en el orden del catálogo
     * @throws DomainValidationException si alguno no existe o no es complemento
     */
    private List<SecurityMechanism> addOns(final Set<String> ids) {
        ids.stream().sorted().forEach(id -> chosen(id, SecurityRole.ADD_ON));
        return mechanisms.withRole(SecurityRole.ADD_ON).stream()
                .filter(mechanism -> ids.contains(mechanism.id()))
                .toList();
    }

    /**
     * Rechaza los pares de métodos elegidos que no se pueden combinar.
     *
     * @param chosen métodos elegidos
     * @throws DomainValidationException si hay choques; el mensaje los lista todos
     */
    private void rejectConflicts(final List<SecurityMechanism> chosen) {
        List<String> pairs = new ArrayList<>();
        for (int i = 0; i < chosen.size(); i++) {
            for (int j = i + 1; j < chosen.size(); j++) {
                if (mechanisms.conflict(chosen.get(i).id(), chosen.get(j).id())) {
                    pairs.add(chosen.get(i).name() + " y " + chosen.get(j).name());
                }
            }
        }
        if (!pairs.isEmpty()) {
            throw new DomainValidationException("No se pueden combinar: " + String.join("; ", pairs));
        }
    }

    /**
     * Rechaza los métodos que no funcionan en un proyecto reactivo.
     *
     * @param chosen   métodos elegidos
     * @param reactive si el proyecto es reactivo
     * @throws DomainValidationException si alguno no funciona; el mensaje los nombra
     */
    private static void rejectNonReactive(final List<SecurityMechanism> chosen, final boolean reactive) {
        if (!reactive) {
            return;
        }
        List<SecurityMechanism> servletOnly = chosen.stream().filter(m -> !m.reactiveSupported()).toList();
        if (!servletOnly.isEmpty()) {
            throw new DomainValidationException("Estos métodos no funcionan en un proyecto reactivo: "
                    + names(servletOnly));
        }
    }

    /**
     * Busca el servidor de autorización propio y verifica que se pueda usar.
     *
     * @param reactive si el proyecto es reactivo
     * @return el método del servidor de autorización
     * @throws DomainValidationException si el catálogo no lo tiene o el proyecto es reactivo
     */
    private SecurityMechanism ownServer(final boolean reactive) {
        SecurityMechanism server = mechanisms.withRole(SecurityRole.AUTHORIZATION_SERVER).stream()
                .findFirst()
                .orElseThrow(() -> new DomainValidationException(
                        "El catálogo no tiene servidor de autorización propio; elija un proveedor externo"));
        if (reactive && !server.reactiveSupported()) {
            throw new DomainValidationException("El servidor de autorización propio no funciona en proyectos "
                    + "reactivos; elija un proveedor externo o un proyecto no reactivo");
        }
        return server;
    }

    /**
     * Busca el proveedor elegido y verifica que sirva para lo que se usa y
     * que no reciba datos que no pide.
     *
     * @param external proveedor elegido con sus datos
     * @param api      autenticación de la API, o {@code null}
     * @param login    inicio de sesión, o {@code null}
     * @return el proveedor
     * @throws DomainValidationException si no existe, no sirve para algún
     *                                   uso o recibe datos que no usa
     */
    private IdentityProvider provider(final TokenIssuer.ExternalProvider external, final SecurityMechanism api,
                                      final SecurityMechanism login) {
        IdentityProvider provider = providers.findProvider(external.providerId())
                .orElseThrow(() -> new DomainValidationException("No existe el proveedor de identidad '"
                        + external.providerId() + "'"));
        if (needsIssuer(api) && !provider.supportsResourceServer()) {
            throw new DomainValidationException("El proveedor " + provider.name()
                    + " no emite tokens que la API pueda validar; elija otro proveedor o el servidor propio");
        }
        if (needsIssuer(login) && !provider.supportsLogin()) {
            throw new DomainValidationException("El proveedor " + provider.name()
                    + " no sirve para el inicio de sesión");
        }
        Set<String> declared = provider.parameters().stream().map(ProviderParameter::key).collect(Collectors.toSet());
        List<String> unused = external.values().keySet().stream().filter(key -> !declared.contains(key))
                .sorted().toList();
        if (!unused.isEmpty()) {
            throw new DomainValidationException("Datos que el proveedor " + provider.name() + " no usa: " + unused);
        }
        return provider;
    }

    /**
     * Indica si un método elegido necesita emisor.
     *
     * @param mechanism método, o {@code null}
     * @return {@code true} si existe y necesita emisor
     */
    private static boolean needsIssuer(final SecurityMechanism mechanism) {
        return mechanism != null && mechanism.requiresIssuer();
    }

    /**
     * Nombres de unos métodos, para los mensajes.
     *
     * @param list métodos
     * @return nombres en el mismo orden
     */
    private static List<String> names(final List<SecurityMechanism> list) {
        return list.stream().map(SecurityMechanism::name).toList();
    }
}