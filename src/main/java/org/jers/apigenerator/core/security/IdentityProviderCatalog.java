package org.jers.apigenerator.core.security;

import org.jers.apigenerator.core.common.DomainValidationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Catálogo de proveedores de identidad. Es inmutable y garantiza ids y nombres únicos.
 *
 * @param providers proveedores en el orden en que se muestran; puede estar vacía
 */
public record IdentityProviderCatalog(List<IdentityProvider> providers) {

    /**
     * Valida el catálogo y protege su lista.
     *
     * @throws NullPointerException      si la lista o alguno de sus elementos es {@code null}
     * @throws DomainValidationException si hay ids o nombres repetidos
     */
    public IdentityProviderCatalog {
        Objects.requireNonNull(providers, "providers es obligatoria");
        providers = List.copyOf(providers);
        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (IdentityProvider provider : providers) {
            if (!ids.add(provider.id())) {
                throw new DomainValidationException("Id de proveedor repetido: " + provider.id());
            }
            if (!names.add(provider.name())) {
                throw new DomainValidationException("Nombre de proveedor repetido: " + provider.name());
            }
        }
    }

    /**
     * Busca un proveedor por su id.
     *
     * @param id identificador del proveedor
     * @return el proveedor, o vacío si no existe
     */
    public Optional<IdentityProvider> findProvider(final String id) {
        return providers.stream().filter(provider -> provider.id().equals(id)).findFirst();
    }

    /**
     * Devuelve un catálogo nuevo con el proveedor agregado al final.
     *
     * @param provider proveedor a agregar
     * @return catálogo nuevo, validado
     * @throws DuplicateIdentityProviderException si ya existe uno con ese id
     * @throws DomainValidationException          si el nombre ya existe
     */
    public IdentityProviderCatalog withProviderAdded(final IdentityProvider provider) {
        Objects.requireNonNull(provider, "provider es obligatorio");
        if (findProvider(provider.id()).isPresent()) {
            throw new DuplicateIdentityProviderException(provider.id());
        }
        List<IdentityProvider> next = new ArrayList<>(providers);
        next.add(provider);
        return new IdentityProviderCatalog(next);
    }

    /**
     * Devuelve un catálogo nuevo en el que el proveedor con el mismo id se
     * reemplaza, conservando su posición.
     *
     * @param provider nueva versión del proveedor
     * @return catálogo nuevo, validado
     * @throws IdentityProviderNotFoundException si no existe uno con ese id
     * @throws DomainValidationException         si el nombre ya existe
     */
    public IdentityProviderCatalog withProviderReplaced(final IdentityProvider provider) {
        Objects.requireNonNull(provider, "provider es obligatorio");
        if (findProvider(provider.id()).isEmpty()) {
            throw new IdentityProviderNotFoundException(provider.id());
        }
        return new IdentityProviderCatalog(providers.stream()
                .map(current -> current.id().equals(provider.id()) ? provider : current)
                .toList());
    }
}