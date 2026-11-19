package org.jers.apigenerator.core.catalog;

import org.jers.apigenerator.core.common.DomainValidationException;
import org.jers.apigenerator.core.common.Preconditions;

import java.util.List;
import java.util.Objects;

/**
 * Conjunto de artefactos que el usuario selecciona como una unidad; por
 * ejemplo, "Spring Data" agrupa JPA, R2DBC y sus starters de pruebas.
 *
 * <p>Es inmutable: la lista de artefactos se copia al construirse y no puede
 * modificarse. Lo que el usuario marca en pantalla no se guarda aquí, sino en
 * la selección del proyecto.</p>
 * @
 * @param id                identificador estable; no cambia aunque cambie el nombre
 * @param name              nombre visible en la interfaz; se puede renombrar
 * @param category          categoría que decide la sección y las reglas del grupo
 * @param dependencies      artefactos del grupo; nunca vacía ni modificable
 * @param selectedByDefault {@code true} si el grupo aparece marcado al abrir
 *                          la pantalla
 * @param requiresDatabase  {@code true} si sus artefactos necesitan una base
 *                          de datos para arrancar, como Spring Data JPA
 */
public record DependencyGroup(
        String id,
        String name,
        DependencyCategory category,
        List<Dependency> dependencies,
        boolean selectedByDefault,
        boolean requiresDatabase) {

    /**
     * Valida el grupo y protege su lista de artefactos.
     *
     * @throws DomainValidationException si el id o el nombre no son válidos o
     *                                   el grupo no tiene artefactos
     * @throws NullPointerException      si la categoría, la lista o alguno de
     *                                   sus elementos es {@code null}
     */
    public DependencyGroup {
        Preconditions.requireSlug(id, "id");
        Preconditions.requireText(name, "name");
        Objects.requireNonNull(category, "category es obligatoria");
        Objects.requireNonNull(dependencies, "dependencies es obligatoria");
        if (dependencies.isEmpty()) {
            throw new DomainValidationException("El grupo '" + name + "' no tiene dependencias");
        }
        dependencies = List.copyOf(dependencies);
    }

    /**
     * Crea un grupo que no necesita base de datos.
     *
     * @param id                identificador estable
     * @param name              nombre visible
     * @param category          categoría del grupo
     * @param dependencies      artefactos del grupo
     * @param selectedByDefault si aparece marcado al abrir la pantalla
     */
    public DependencyGroup(final String id, final String name, final DependencyCategory category,
                           final List<Dependency> dependencies, final boolean selectedByDefault) {
        this(id, name, category, dependencies, selectedByDefault, false);
    }

    /**
     * Devuelve los artefactos que se incluyen siempre que el grupo está
     * seleccionado, sin aplicar reglas condicionales como la reactiva.
     *
     * @return lista inmutable, posiblemente vacía
     */
    public List<Dependency> defaultDependencies() {
        return dependencies.stream()
                .filter(Dependency::includedByDefault)
                .toList();
    }
}