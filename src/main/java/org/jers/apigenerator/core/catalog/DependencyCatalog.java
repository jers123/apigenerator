package org.jers.apigenerator.core.catalog;

import org.jers.apigenerator.core.common.DomainValidationException;

import java.util.*;

/**
 * Catálogo completo de grupos de dependencias que ofrece el generador.
 *
 * <p>Es inmutable y garantiza que no hay dos grupos con el mismo id ni con el
 * mismo nombre.</p>
 *
 * @param groups grupos del catálogo, en el orden en que se muestran; nunca
 *               vacía ni modificable
 */
public record DependencyCatalog(List<DependencyGroup> groups) {

    /**
     * Valida el catálogo y protege su lista de grupos.
     *
     * @throws DomainValidationException si el catálogo está vacío o tiene
     *                                   grupos con id o nombre repetido
     * @throws NullPointerException      si la lista o alguno de sus grupos es
     *                                   {@code null}
     */
    public DependencyCatalog {
        Objects.requireNonNull(groups, "groups es obligatoria");
        if (groups.isEmpty()) {
            throw new DomainValidationException("El catálogo de dependencias está vacío");
        }
        groups = List.copyOf(groups);
        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (DependencyGroup group : groups) {
            if (!ids.add(group.id())) {
                throw new DomainValidationException("Id de grupo repetido en el catálogo: " + group.id());
            }
            if (!names.add(group.name())) {
                throw new DomainValidationException("Nombre de grupo repetido en el catálogo: " + group.name());
            }
        }
    }

    /**
     * Busca un grupo por su id.
     *
     * @param id identificador del grupo
     * @return el grupo, o vacío si no existe
     */
    public Optional<DependencyGroup> findGroup(final String id) {
        return groups.stream()
                .filter(group -> group.id().equals(id))
                .findFirst();
    }

    /**
     * Devuelve los grupos de una categoría, conservando el orden del catálogo.
     *
     * @param category categoría a filtrar
     * @return lista inmutable, posiblemente vacía
     */
    public List<DependencyGroup> groupsIn(final DependencyCategory category) {
        return groups.stream()
                .filter(group -> group.category() == category)
                .toList();
    }

    /**
     * Devuelve un catálogo nuevo con el grupo agregado al final. Este
     * catálogo no cambia.
     *
     * @param group grupo a agregar
     * @return catálogo nuevo, validado
     * @throws DuplicateDependencyGroupException si ya existe un grupo con ese id
     * @throws DomainValidationException         si otro grupo ya usa ese nombre
     */
    public DependencyCatalog withGroupAdded(final DependencyGroup group) {
        Objects.requireNonNull(group, "group es obligatorio");
        if (findGroup(group.id()).isPresent()) {
            throw new DuplicateDependencyGroupException(group.id());
        }
        List<DependencyGroup> next = new ArrayList<>(groups);
        next.add(group);
        return new DependencyCatalog(next);
    }

    /**
     * Devuelve un catálogo nuevo en el que el grupo con el mismo id se
     * reemplaza, conservando su posición. Este catálogo no cambia.
     *
     * @param group nueva versión del grupo
     * @return catálogo nuevo, validado
     * @throws DependencyGroupNotFoundException si no existe un grupo con ese id
     * @throws DomainValidationException        si otro grupo ya usa ese nombre
     */
    public DependencyCatalog withGroupReplaced(final DependencyGroup group) {
        Objects.requireNonNull(group, "group es obligatorio");
        if (findGroup(group.id()).isEmpty()) {
            throw new DependencyGroupNotFoundException(group.id());
        }
        return new DependencyCatalog(groups.stream()
                .map(current -> current.id().equals(group.id()) ? group : current)
                .toList());
    }

    /**
     * Devuelve un catálogo nuevo sin el grupo indicado. Este catálogo no
     * cambia.
     *
     * @param id identificador del grupo a quitar
     * @return catálogo nuevo, validado
     * @throws DependencyGroupNotFoundException si no existe un grupo con ese id
     * @throws DomainValidationException        si el catálogo quedaría vacío
     */
    public DependencyCatalog withoutGroup(final String id) {
        if (findGroup(id).isEmpty()) {
            throw new DependencyGroupNotFoundException(id);
        }
        return new DependencyCatalog(groups.stream()
                .filter(group -> !group.id().equals(id))
                .toList());
    }
}