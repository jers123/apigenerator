package org.jers.apigenerator.application.catalog;

import lombok.RequiredArgsConstructor;
import org.jers.apigenerator.core.catalog.DependencyCatalogRepository;
import org.jers.apigenerator.core.catalog.DependencyCategory;
import org.jers.apigenerator.core.catalog.DependencyGroup;
import org.jers.apigenerator.core.catalog.DependencyGroupNotFoundException;
import org.jers.apigenerator.core.common.DomainValidationException;
import org.springframework.stereotype.Service;

/**
 * Casos de uso para administrar los grupos del catálogo de forma
 * independiente: consultar, crear y editar.
 *
 * <p>Los grupos de base de datos quedan fuera: cada uno pertenece a un motor
 * y se crea o edita junto con él, para que ningún motor se quede sin su
 * driver.</p>
 */
@Service
@RequiredArgsConstructor
public class DependencyGroupService {

    /** Almacenamiento del catálogo. */
    private final DependencyCatalogRepository repository;

    /**
     * Busca un grupo por su id.
     *
     * @param id identificador del grupo
     * @return el grupo
     * @throws DependencyGroupNotFoundException si no existe
     */
    public DependencyGroup findById(final String id) {
        return repository.catalog().findGroup(id).orElseThrow(() -> new DependencyGroupNotFoundException(id));
    }

    /**
     * Agrega un grupo al final del catálogo.
     *
     * @param group grupo a crear
     * @return el grupo guardado
     * @throws DomainValidationException si es de base de datos o su nombre ya existe
     * @throws org.jers.apigenerator.core.catalog.DuplicateDependencyGroupException
     *         si ya existe un grupo con ese id
     */
    public DependencyGroup create(final DependencyGroup group) {
        requireStandalone(group);
        repository.modify(catalog -> catalog.withGroupAdded(group));
        return group;
    }

    /**
     * Reemplaza un grupo existente. El id no se puede cambiar.
     *
     * @param id    id del grupo a editar, tomado de la URL
     * @param group nueva versión del grupo
     * @return el grupo guardado
     * @throws DomainValidationException        si el id no coincide, si el grupo
     *                                          actual o el nuevo es de base de
     *                                          datos, o si el nombre ya existe
     * @throws DependencyGroupNotFoundException si no existe
     */
    public DependencyGroup update(final String id, final DependencyGroup group) {
        if (!id.equals(group.id())) {
            throw new DomainValidationException("El id de la URL ('" + id + "') no coincide con el del cuerpo ('"
                    + group.id() + "'); el id de un grupo no se puede cambiar");
        }
        requireStandalone(group);
        repository.modify(catalog -> {
            requireStandalone(catalog.findGroup(id).orElseThrow(() -> new DependencyGroupNotFoundException(id)));
            return catalog.withGroupReplaced(group);
        });
        return group;
    }

    /**
     * Rechaza los grupos de base de datos, que se administran con su motor.
     *
     * @param group grupo a verificar
     * @throws DomainValidationException si es de la categoría
     *                                   {@link DependencyCategory#DATABASE}
     */
    private static void requireStandalone(final DependencyGroup group) {
        if (group.category() == DependencyCategory.DATABASE) {
            throw new DomainValidationException("El grupo '" + group.id() + "' es de base de datos: "
                    + "se crea y edita junto con su motor, en /api/v1/databases");
        }
    }
}