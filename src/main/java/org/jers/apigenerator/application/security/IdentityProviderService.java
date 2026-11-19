package org.jers.apigenerator.application.security;

import lombok.RequiredArgsConstructor;
import org.jers.apigenerator.core.common.DomainValidationException;
import org.jers.apigenerator.core.security.IdentityProvider;
import org.jers.apigenerator.core.security.IdentityProviderNotFoundException;
import org.jers.apigenerator.core.security.IdentityProviderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Casos de uso para administrar los proveedores de identidad: consultar, crear y editar.
 */
@Service
@RequiredArgsConstructor
public class IdentityProviderService {

    /** Almacenamiento de los proveedores. */
    private final IdentityProviderRepository repository;

    /**
     * Devuelve todos los proveedores.
     *
     * @return lista inmutable, en el orden del catálogo
     */
    public List<IdentityProvider> findAll() {
        return repository.catalog().providers();
    }

    /**
     * Busca un proveedor por su id.
     *
     * @param id identificador del proveedor
     * @return el proveedor
     * @throws IdentityProviderNotFoundException si no existe
     */
    public IdentityProvider findById(final String id) {
        return repository.catalog().findProvider(id).orElseThrow(() -> new IdentityProviderNotFoundException(id));
    }

    /**
     * Agrega un proveedor al final del catálogo.
     *
     * @param provider proveedor a crear
     * @return el proveedor guardado
     * @throws org.jers.apigenerator.core.security.DuplicateIdentityProviderException
     *         si ya existe uno con ese id
     * @throws DomainValidationException si su nombre ya existe
     */
    public IdentityProvider create(final IdentityProvider provider) {
        repository.modify(catalog -> catalog.withProviderAdded(provider));
        return provider;
    }

    /**
     * Reemplaza un proveedor existente. El id no se puede cambiar.
     *
     * @param id       id del proveedor a editar, tomado de la URL
     * @param provider nueva versión del proveedor
     * @return el proveedor guardado
     * @throws DomainValidationException         si el id no coincide o el nombre ya existe
     * @throws IdentityProviderNotFoundException si no existe
     */
    public IdentityProvider update(final String id, final IdentityProvider provider) {
        if (!id.equals(provider.id())) {
            throw new DomainValidationException("El id de la URL ('" + id + "') no coincide con el del cuerpo ('"
                    + provider.id() + "'); el id de un proveedor no se puede cambiar");
        }
        repository.modify(catalog -> catalog.withProviderReplaced(provider));
        return provider;
    }
}