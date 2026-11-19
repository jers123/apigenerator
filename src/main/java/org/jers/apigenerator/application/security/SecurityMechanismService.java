package org.jers.apigenerator.application.security;

import lombok.RequiredArgsConstructor;
import org.jers.apigenerator.core.common.DomainValidationException;
import org.jers.apigenerator.core.security.SecurityCatalogRepository;
import org.jers.apigenerator.core.security.SecurityMechanism;
import org.jers.apigenerator.core.security.SecurityMechanismNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Casos de uso para administrar los métodos de seguridad: consultar, crear y editar.
 */
@Service
@RequiredArgsConstructor
public class SecurityMechanismService {

    /** Almacenamiento del catálogo de seguridad. */
    private final SecurityCatalogRepository repository;

    /**
     * Devuelve todos los métodos.
     *
     * @return lista inmutable, en el orden del catálogo
     */
    public List<SecurityMechanism> findAll() {
        return repository.catalog().mechanisms();
    }

    /**
     * Busca un método por su id.
     *
     * @param id identificador del método
     * @return el método
     * @throws SecurityMechanismNotFoundException si no existe
     */
    public SecurityMechanism findById(final String id) {
        return repository.catalog().findMechanism(id).orElseThrow(() -> new SecurityMechanismNotFoundException(id));
    }

    /**
     * Agrega un método al final del catálogo.
     *
     * @param mechanism método a crear
     * @return el método guardado
     * @throws org.jers.apigenerator.core.security.DuplicateSecurityMechanismException
     *         si ya existe uno con ese id
     * @throws DomainValidationException si su nombre ya existe o el catálogo
     *                                   resultante no es válido
     */
    public SecurityMechanism create(final SecurityMechanism mechanism) {
        repository.modify(catalog -> catalog.withMechanismAdded(mechanism));
        return mechanism;
    }

    /**
     * Reemplaza un método existente. El id no se puede cambiar.
     *
     * @param id        id del método a editar, tomado de la URL
     * @param mechanism nueva versión del método
     * @return el método guardado
     * @throws DomainValidationException          si el id no coincide o el
     *                                            catálogo resultante no es válido
     * @throws SecurityMechanismNotFoundException si no existe
     */
    public SecurityMechanism update(final String id, final SecurityMechanism mechanism) {
        if (!id.equals(mechanism.id())) {
            throw new DomainValidationException("El id de la URL ('" + id + "') no coincide con el del cuerpo ('"
                    + mechanism.id() + "'); el id de un método no se puede cambiar");
        }
        repository.modify(catalog -> catalog.withMechanismReplaced(mechanism));
        return mechanism;
    }
}