package org.jers.apigenerator.core.project;

import org.jers.apigenerator.core.catalog.Dependency;
import org.jers.apigenerator.core.catalog.DependencyCatalog;
import org.jers.apigenerator.core.catalog.DependencyCategory;
import org.jers.apigenerator.core.catalog.DependencyGroup;
import org.jers.apigenerator.core.catalog.DependencyKind;
import org.jers.apigenerator.core.common.DomainValidationException;
import org.jers.apigenerator.core.model.Attribute;
import org.jers.apigenerator.core.model.DomainModel;
import org.jers.apigenerator.core.model.ModelIssue;
import org.jers.apigenerator.core.model.ModelValidation;
import org.jers.apigenerator.core.model.ModelValidator;
import org.jers.apigenerator.core.security.IdentityProviderCatalog;
import org.jers.apigenerator.core.security.ResolvedSecurity;
import org.jers.apigenerator.core.security.SecurityCatalog;
import org.jers.apigenerator.core.security.SecurityResolver;
import org.jers.apigenerator.core.selection.DependencySelector;
import org.jers.apigenerator.core.selection.ResolvedSelection;
import org.jers.apigenerator.core.selection.SelectionRequest;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Convierte lo que pidió el usuario en el plan del proyecto: aplica las
 * reglas de selección, de seguridad y del modelo, y decide la versión final
 * de cada dependencia.
 *
 * <ul>
 *   <li>La versión de Spring Boot es la del parent: la elegida o la del catálogo.</li>
 *   <li>Los artefactos de Spring Boot no llevan versión propia y no se les
 *       puede fijar una.</li>
 *   <li>El resto usa la versión elegida o, si no hay, la del catálogo;
 *       {@code null} significa que la gestiona Spring Boot o un BOM.</li>
 *   <li>No se aceptan versiones para artefactos que no están en el proyecto.</li>
 *   <li>Los grupos de base de datos no se eligen directamente: entran con
 *       la base de datos elegida.</li>
 *   <li>Sin base de desarrollo no se pueden elegir grupos que la necesiten.</li>
 *   <li>Los grupos del catálogo no pueden traer librerías de seguridad: esas
 *       entran con los métodos elegidos en el paso Seguridad.</li>
 *   <li>Un modelo con errores impide generar el proyecto.</li>
 *   <li>La estrategia de id SEQUENCE necesita motores con secuencias.</li>
 *   <li>El modelo agrega solo los grupos de persistencia y de validación que necesita.</li>
 * </ul>
 *
 * <p>No tiene estado mutable: una misma instancia puede atender peticiones
 * concurrentes.</p>
 */
public final class ProjectPlanner {

    /** Cuántos errores del modelo se muestran en el mensaje de rechazo. */
    private static final int SHOWN_MODEL_ERRORS = 3;

    /** Validador del modelo; no tiene estado y se reutiliza. */
    private static final ModelValidator MODEL_VALIDATOR = new ModelValidator();

    /** Catálogo con el que se resuelven los proyectos. */
    private final DependencyCatalog catalog;

    /** Catálogo de métodos de seguridad; aporta las librerías de seguridad. */
    private final SecurityCatalog securityCatalog;

    /** Valida la seguridad elegida contra los catálogos. */
    private final SecurityResolver securityResolver;

    /** Grupos que el modelo agrega solo. */
    private final ModelDependencyGroups modelGroups;

    /**
     * Crea un planificador para unos catálogos.
     *
     * @param catalog         catálogo de dependencias vigente
     * @param securityCatalog catálogo de métodos de seguridad vigente
     * @param providers       catálogo de proveedores de identidad vigente
     * @param modelGroups     grupos que el modelo agrega solo
     * @throws NullPointerException si algún argumento es {@code null}
     */
    public ProjectPlanner(final DependencyCatalog catalog, final SecurityCatalog securityCatalog,
                          final IdentityProviderCatalog providers, final ModelDependencyGroups modelGroups) {
        this.catalog = Objects.requireNonNull(catalog, "catalog es obligatorio");
        this.securityCatalog = Objects.requireNonNull(securityCatalog, "securityCatalog es obligatorio");
        this.securityResolver = new SecurityResolver(securityCatalog, providers);
        this.modelGroups = Objects.requireNonNull(modelGroups, "modelGroups es obligatorio");
    }

    /**
     * Crea un planificador con los grupos del modelo de fábrica.
     *
     * @param catalog         catálogo de dependencias vigente
     * @param securityCatalog catálogo de métodos de seguridad vigente
     * @param providers       catálogo de proveedores de identidad vigente
     * @throws NullPointerException si algún catálogo es {@code null}
     */
    public ProjectPlanner(final DependencyCatalog catalog, final SecurityCatalog securityCatalog,
                          final IdentityProviderCatalog providers) {
        this(catalog, securityCatalog, providers, ModelDependencyGroups.defaults());
    }

    /**
     * Calcula el plan de un proyecto.
     *
     * @param spec proyecto pedido por el usuario
     * @return plan con las dependencias finales, sus versiones y la seguridad
     * @throws org.jers.apigenerator.core.selection.UnknownDependencyGroupException
     *                                   si se pide un grupo inexistente
     * @throws DomainValidationException si una versión no se puede aplicar, el
     *                                   proyecto no tiene parent con versión,
     *                                   se eligió un grupo no permitido, la
     *                                   seguridad no cumple sus reglas o el
     *                                   modelo tiene errores
     */
    public ProjectPlan plan(final ProjectSpec spec) {
        rejectInvalidModel(spec.model());
        rejectSequencesWithoutSupport(spec);
        Set<String> requiredByModel = groupsRequiredByModel(spec);
        requireGroupsInCatalog(requiredByModel);

        ResolvedSelection selection = new DependencySelector(catalog).select(withGroups(spec, requiredByModel));
        rejectDatabaseGroupsChosenDirectly(spec);
        rejectGroupsThatNeedADatabase(spec);
        rejectGroupsWithSecurityArtifacts(selection.groups());
        ResolvedSecurity security = securityResolver.resolve(spec.security(), spec.build().reactive());

        List<Dependency> dependencies = Stream.concat(selection.dependencies().stream(),
                security.dependencies().stream()).toList();
        Map<String, String> overrides = spec.versionOverrides();
        rejectOverridesOutsideTheProject(dependencies, overrides);
        rejectOverridesForSpringBootArtifacts(dependencies, overrides);

        List<PlannedDependency> planned = dependencies.stream()
                .map(dependency -> new PlannedDependency(dependency, versionOf(dependency, overrides)))
                .toList();
        return new ProjectPlan(spec, springBootVersion(planned), planned, security);
    }

    /**
     * Grupos que el modelo agrega solo al proyecto.
     *
     * @param spec proyecto pedido
     * @return ids de los grupos, en orden: persistencia y validación
     */
    public Set<String> groupsRequiredByModel(final ProjectSpec spec) {
        DomainModel model = spec.model();
        Set<String> ids = new LinkedHashSet<>();
        if (!model.entities().isEmpty() && spec.database() != null) {
            ids.add(modelGroups.persistenceGroupId());
        }
        if (needsValidation(model)) {
            ids.add(modelGroups.validationGroupId());
        }
        return ids;
    }

    /**
     * Indica si el modelo genera anotaciones de Jakarta Validation.
     *
     * @param model modelo
     * @return {@code true} si algún atributo, propio o de una superclase, o
     *         alguna relación es obligatorio o tiene restricciones
     */
    private static boolean needsValidation(final DomainModel model) {
        Stream<Attribute> inherited = model.superclasses().stream()
                .flatMap(superclass -> superclass.attributes().stream());
        Stream<Attribute> own = model.entities().stream().flatMap(entity -> entity.attributes().stream());
        boolean attributes = Stream.concat(inherited, own)
                .anyMatch(attribute -> attribute.required() || attribute.constraints().hasAny());
        boolean relations = model.entities().stream()
                .flatMap(entity -> entity.relations().stream())
                .anyMatch(relation -> relation.required());
        return attributes || relations;
    }

    /**
     * Rechaza el proyecto si el modelo tiene errores; los avisos no cuentan.
     *
     * @param model modelo
     * @throws DomainValidationException con el total de errores y los primeros
     */
    private static void rejectInvalidModel(final DomainModel model) {
        ModelValidation validation = MODEL_VALIDATOR.validate(model);
        if (!validation.hasErrors()) {
            return;
        }
        List<ModelIssue> errors = validation.errors();
        String shown = errors.stream()
                .limit(SHOWN_MODEL_ERRORS)
                .map(issue -> issue.location() + ": " + issue.message())
                .collect(Collectors.joining("; "));
        String more = errors.size() > SHOWN_MODEL_ERRORS
                ? " (y " + (errors.size() - SHOWN_MODEL_ERRORS) + " más)"
                : "";
        throw new DomainValidationException("El modelo tiene " + errors.size()
                + (errors.size() == 1 ? " error" : " errores") + "; corríjalos en el paso Modelo. " + shown + more);
    }

    /**
     * Rechaza la estrategia SEQUENCE si alguna base elegida no tiene secuencias.
     *
     * @param spec proyecto pedido
     * @throws DomainValidationException nombrando el motor sin secuencias
     */
    private static void rejectSequencesWithoutSupport(final ProjectSpec spec) {
        if (!spec.model().idSettings().generation().needsSequences()) {
            return;
        }
        for (DatabaseConnection connection : spec.databases()) {
            if (!connection.engine().supportsSequences()) {
                throw new DomainValidationException("La estrategia de id «Secuencia» necesita un motor con "
                        + "secuencias, y " + connection.engine().label() + " no las tiene: use Autoincremento "
                        + "o elija otro motor");
            }
        }
    }

    /**
     * Verifica que los grupos que necesita el modelo existan en el catálogo.
     *
     * @param groupIds grupos requeridos por el modelo
     * @throws DomainValidationException nombrando el grupo y la propiedad que lo configura
     */
    private void requireGroupsInCatalog(final Set<String> groupIds) {
        for (String id : groupIds) {
            if (catalog.findGroup(id).isEmpty()) {
                String property = id.equals(modelGroups.persistenceGroupId())
                        ? "app.model.persistence-group"
                        : "app.model.validation-group";
                throw new DomainValidationException("El modelo necesita el grupo '" + id + "' (" + property
                        + "), pero no existe en el catálogo");
            }
        }
    }

    /**
     * Petición de selección del proyecto con los grupos del modelo agregados.
     *
     * @param spec  proyecto pedido
     * @param extra grupos que agrega el modelo
     * @return petición para el selector
     */
    private static SelectionRequest withGroups(final ProjectSpec spec, final Set<String> extra) {
        SelectionRequest base = spec.selectionRequest();
        Set<String> groupIds = new HashSet<>(base.groupIds());
        groupIds.addAll(extra);
        return new SelectionRequest(groupIds, base.buildTool(), base.reactive());
    }

    /**
     * Versión final de una dependencia.
     *
     * @param dependency artefacto del proyecto
     * @param overrides  versiones elegidas por el usuario
     * @return versión elegida, la del catálogo o {@code null} si es gestionada
     */
    private static String versionOf(final Dependency dependency, final Map<String, String> overrides) {
        if (dependency.followsSpringBootVersion()) {
            return null;
        }
        return overrides.getOrDefault(dependency.coordinates(), dependency.version());
    }

    /**
     * Toma la versión de Spring Boot del parent.
     *
     * @param planned dependencias con su versión final
     * @return versión del parent
     * @throws DomainValidationException si no hay parent o no tiene versión
     */
    private static String springBootVersion(final List<PlannedDependency> planned) {
        return planned.stream()
                .filter(dependency -> dependency.dependency().kind() == DependencyKind.PARENT)
                .map(PlannedDependency::version)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new DomainValidationException("El proyecto no tiene versión de Spring Boot: "
                        + "incluya el grupo que aporta el parent y verifique que el parent tenga versión"));
    }

    /**
     * Rechaza las versiones de artefactos que no forman parte del proyecto.
     *
     * @param dependencies dependencias finales
     * @param overrides    versiones elegidas
     * @throws DomainValidationException si alguna clave no corresponde; el
     *                                   mensaje las lista todas, ordenadas
     */
    private static void rejectOverridesOutsideTheProject(final List<Dependency> dependencies,
                                                         final Map<String, String> overrides) {
        Set<String> present = dependencies.stream().map(Dependency::coordinates).collect(Collectors.toSet());
        List<String> unknown = overrides.keySet().stream().filter(key -> !present.contains(key)).sorted().toList();
        if (!unknown.isEmpty()) {
            throw new DomainValidationException("Versiones para artefactos que no forman parte del proyecto: "
                    + unknown);
        }
    }

    /**
     * Rechaza las versiones de artefactos que siguen al parent de Spring Boot.
     *
     * @param dependencies dependencias finales
     * @param overrides    versiones elegidas
     * @throws DomainValidationException si se intenta fijar alguna; el
     *                                   mensaje las lista todas, ordenadas
     */
    private static void rejectOverridesForSpringBootArtifacts(final List<Dependency> dependencies,
                                                              final Map<String, String> overrides) {
        List<String> fixed = dependencies.stream()
                .filter(Dependency::followsSpringBootVersion)
                .map(Dependency::coordinates)
                .filter(overrides::containsKey)
                .distinct()
                .sorted()
                .toList();
        if (!fixed.isEmpty()) {
            throw new DomainValidationException("Estos artefactos siguen la versión de Spring Boot y no se les "
                    + "puede fijar una: " + fixed);
        }
    }

    /**
     * Grupos que eligió el usuario, tal como están en el catálogo.
     *
     * @param spec proyecto pedido
     * @return grupos elegidos que existen, en el orden del catálogo
     */
    private List<DependencyGroup> chosenGroups(final ProjectSpec spec) {
        return spec.dependencyGroupIds().stream()
                .map(catalog::findGroup)
                .flatMap(Optional::stream)
                .sorted((a, b) -> Integer.compare(catalog.groups().indexOf(a), catalog.groups().indexOf(b)))
                .toList();
    }

    /**
     * Rechaza los grupos de base de datos elegidos como dependencias: entran
     * solos con la base de datos elegida.
     *
     * @param spec proyecto pedido
     * @throws DomainValidationException si se eligió alguno; el mensaje los nombra
     */
    private void rejectDatabaseGroupsChosenDirectly(final ProjectSpec spec) {
        List<String> drivers = chosenGroups(spec).stream()
                .filter(group -> group.category() == DependencyCategory.DATABASE)
                .map(DependencyGroup::name)
                .toList();
        if (!drivers.isEmpty()) {
            throw new DomainValidationException("Los drivers se agregan al elegir la base de datos, no como "
                    + "dependencias: " + drivers);
        }
    }

    /**
     * Rechaza los grupos que necesitan base de datos cuando el proyecto no tiene.
     *
     * @param spec proyecto pedido
     * @throws DomainValidationException si se eligió alguno; el mensaje los nombra
     */
    private void rejectGroupsThatNeedADatabase(final ProjectSpec spec) {
        if (spec.database() != null) {
            return;
        }
        List<String> needing = chosenGroups(spec).stream()
                .filter(DependencyGroup::requiresDatabase)
                .map(DependencyGroup::name)
                .toList();
        if (!needing.isEmpty()) {
            throw new DomainValidationException("Estos grupos necesitan una base de datos de desarrollo: "
                    + needing + "; elija una base de datos o quítelos");
        }
    }

    /**
     * Rechaza los grupos activos que traen artefactos del catálogo de
     * seguridad, para que esas librerías solo entren por el paso Seguridad.
     *
     * @param groups grupos activos de la selección
     * @throws DomainValidationException si hay alguno; el mensaje los nombra
     */
    private void rejectGroupsWithSecurityArtifacts(final List<DependencyGroup> groups) {
        Set<String> securityArtifacts = securityCatalog.mechanisms().stream()
                .flatMap(mechanism -> mechanism.dependencies().stream())
                .map(Dependency::coordinates)
                .collect(Collectors.toSet());
        List<String> offending = groups.stream()
                .filter(group -> group.dependencies().stream()
                        .map(Dependency::coordinates)
                        .anyMatch(securityArtifacts::contains))
                .map(DependencyGroup::name)
                .toList();
        if (!offending.isEmpty()) {
            throw new DomainValidationException("Estos grupos traen librerías de seguridad, que se eligen en el "
                    + "paso Seguridad: " + offending + "; quítelas del grupo o no lo elija");
        }
    }
}