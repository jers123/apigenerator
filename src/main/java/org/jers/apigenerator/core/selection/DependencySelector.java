package org.jers.apigenerator.core.selection;

import org.jers.apigenerator.core.catalog.Dependency;
import org.jers.apigenerator.core.catalog.DependencyCatalog;
import org.jers.apigenerator.core.catalog.DependencyCategory;
import org.jers.apigenerator.core.catalog.DependencyGroup;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Aplica las reglas de negocio que deciden qué dependencias lleva un proyecto.
 *
 * <ul>
 *   <li>El grupo de la herramienta de build se incluye siempre; el de la otra
 *       herramienta se descarta aunque se haya pedido.</li>
 *   <li>Sin modo reactivo se toman las dependencias incluidas por defecto de
 *       cada grupo.</li>
 *   <li>En modo reactivo, los grupos con variantes reactivas las usan en
 *       lugar de las tradicionales; los grupos de base de datos conservan
 *       además el driver JDBC, que necesitan las herramientas de
 *       migración.</li>
 * </ul>
 *
 * <p>No tiene estado mutable: una misma instancia puede atender peticiones
 * concurrentes.</p>
 */
public final class DependencySelector {

    /** Catálogo sobre el que se resuelven las peticiones. */
    private final DependencyCatalog catalog;

    /**
     * Crea un selector para un catálogo.
     *
     * @param catalog catálogo de dependencias disponible
     * @throws NullPointerException si el catálogo es {@code null}
     */
    public DependencySelector(final DependencyCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog es obligatorio");
    }

    /**
     * Resuelve la elección del usuario en la lista final de dependencias.
     *
     * @param request elección del usuario
     * @return grupos activos y dependencias finales, en el orden del catálogo
     * @throws org.jers.apigenerator.core.common.DomainValidationException si se pide un grupo que no existe
     * @throws NullPointerException     si la petición es {@code null}
     */
    public ResolvedSelection select(final SelectionRequest request) {
        Objects.requireNonNull(request, "request es obligatorio");
        rejectUnknownGroups(request.groupIds());

        List<DependencyGroup> groups = catalog.groups().stream()
                .filter(group -> !request.buildTool().excludes(group.category()))
                .filter(group -> isActive(group, request))
                .toList();

        List<Dependency> dependencies = groups.stream()
                .flatMap(group -> applicableDependencies(group, request.reactive()).stream())
                .toList();

        return new ResolvedSelection(groups, dependencies);
    }

    /**
     * Rechaza los nombres de grupo que no existen en el catálogo.
     *
     * @param groupIds nombres pedidos
     * @throws UnknownDependencyGroupException si algún nombre no existe; el mensaje
     *                                  los lista todos, ordenados
     */
    private void rejectUnknownGroups(final Set<String> groupIds) {
        List<String> unknown = groupIds.stream()
                .filter(id -> catalog.findGroup(id).isEmpty())
                .sorted()
                .toList();
        if (!unknown.isEmpty()) {
            throw new UnknownDependencyGroupException(unknown);
        }
    }

    /**
     * Indica si un grupo queda activo: porque es de la herramienta de build o
     * porque el usuario lo pidió.
     *
     * @param group   grupo a evaluar
     * @param request elección del usuario
     * @return {@code true} si el grupo debe incluirse
     */
    private static boolean isActive(final DependencyGroup group, final SelectionRequest request) {
        return group.category() == request.buildTool().getCategory()
                || request.groupIds().contains(group.id());
    }

    /**
     * Devuelve las dependencias que aporta un grupo según el modo reactivo,
     * aplicando las mismas reglas que {@link #select(SelectionRequest)}.
     *
     * @param group    grupo del catálogo
     * @param reactive {@code true} si el proyecto es reactivo
     * @return dependencias del grupo que entrarían al proyecto
     */
    public static List<Dependency> applicableDependencies(final DependencyGroup group, final boolean reactive) {
        List<Dependency> reactiveVariants = group.dependencies().stream()
                .filter(Dependency::reactive)
                .toList();
        if (!reactive || reactiveVariants.isEmpty()) {
            return group.defaultDependencies();
        }
        if (group.category() == DependencyCategory.DATABASE) {
            return Stream.concat(group.defaultDependencies().stream(), reactiveVariants.stream())
                    .toList();
        }
        return reactiveVariants;
    }
}