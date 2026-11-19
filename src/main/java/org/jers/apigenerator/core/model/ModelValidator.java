package org.jers.apigenerator.core.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Revisa un modelo completo y devuelve todos sus problemas a la vez, cada uno
 * con el elemento afectado, para que la pantalla los marque en su fila.
 *
 * <p>Los atributos heredados de las superclases y la auditoría cuentan en
 * toda la cadena de herencia: un nombre o una columna no se puede repetir
 * entre una entidad y sus superclases. Cada choque se informa una sola vez,
 * en el elemento que lo declara.</p>
 *
 * <p>Solo revisa reglas propias del modelo para una base relacional. Las que
 * dependen del motor elegido, como las secuencias, las revisa el plan del
 * proyecto. No tiene estado: una misma instancia sirve para cualquier modelo.</p>
 */
public final class ModelValidator {

    /** Nombre de entidad o superclase: PascalCase, letras y números ASCII. */
    static final Pattern ENTITY_NAME = Pattern.compile("[A-Z][A-Za-z0-9]*");

    /** Nombre de atributo o relación: camelCase, letras y números ASCII. */
    static final Pattern MEMBER_NAME = Pattern.compile("[a-z][A-Za-z0-9]*");

    /** Nombre de tabla, columna o secuencia: snake_case. */
    static final Pattern SQL_NAME = Pattern.compile("[a-z][a-z0-9_]*");

    /** Ruta REST: segmentos en kebab-case que empiezan por barra. */
    static final Pattern RESOURCE_PATH = Pattern.compile("(/[a-z0-9]+(-[a-z0-9]+)*)+");

    /** Largo máximo de un nombre en la base; es el límite de PostgreSQL. */
    static final int MAX_SQL_NAME = 63;

    /** Nombre y columna del id que agrega el modelo. */
    private static final String ID = "id";

    /** Separador de la ubicación legible. */
    private static final String ARROW = " › ";

    /** Ubicación de la configuración del id. */
    private static final String ID_LOCATION = "Id";

    /** Quién usa los nombres y columnas de auditoría, para los mensajes. */
    private static final String AUDIT = "la auditoría";

    /**
     * Valida un modelo.
     *
     * @param model modelo a revisar
     * @return todos los problemas encontrados; vacío si está bien
     * @throws NullPointerException si el modelo es {@code null}
     */
    public ModelValidation validate(final DomainModel model) {
        Objects.requireNonNull(model, "model es obligatorio");
        Issues issues = new Issues();
        ModelDerivations derivations = new ModelDerivations(model);
        checkIdSettings(model, issues);
        checkInternalIds(model, issues);
        checkClassNames(model, issues);
        checkEntityUniqueness(model.entities(), issues);
        for (Superclass superclass : model.superclasses()) {
            checkSuperclass(model, superclass, issues);
        }
        checkUnusedSuperclasses(model, issues);
        for (Entity entity : model.entities()) {
            checkEntity(model, entity, derivations, issues);
            checkColumns(model, entity, derivations, issues);
        }
        checkJoinTables(model, derivations, issues);
        return new ModelValidation(issues.list());
    }

    // ── Id y ids internos ────────────────────────────────────────────

    /**
     * Revisa que el tipo de id sirva como id, sea compatible con su
     * generación y que haya superclases si el id va en la raíz.
     *
     * @param model  modelo
     * @param issues donde se anotan los problemas
     */
    private static void checkIdSettings(final DomainModel model, final Issues issues) {
        IdSettings settings = model.idSettings();
        ScalarType type = settings.type();
        if (!type.isIdCapable()) {
            issues.error(null, ID_LOCATION, "El tipo " + type.getJavaType()
                    + " no puede ser id: use Long, Integer, UUID o String");
        } else if (!settings.generation().accepts(type)) {
            String compatible = settings.generation().getCompatibleTypes().stream()
                    .map(ScalarType::getJavaType).sorted().collect(Collectors.joining(" o "));
            issues.error(null, ID_LOCATION, "La estrategia " + settings.generation().getLabel()
                    + " necesita un id de tipo " + compatible);
        }
        if (settings.placement() == IdPlacement.ROOT_SUPERCLASS && model.superclasses().isEmpty()) {
            issues.error(null, ID_LOCATION, "El id va en la superclase raíz, pero el modelo no tiene superclases: "
                    + "cree una o ponga el id en cada entidad");
        }
    }

    /**
     * Revisa que ningún id interno se repita.
     *
     * @param model  modelo
     * @param issues donde se anotan los problemas
     */
    private static void checkInternalIds(final DomainModel model, final Issues issues) {
        List<String> ids = new ArrayList<>();
        for (Superclass superclass : model.superclasses()) {
            ids.add(superclass.id());
            superclass.attributes().forEach(attribute -> ids.add(attribute.id()));
        }
        for (Entity entity : model.entities()) {
            ids.add(entity.id());
            entity.attributes().forEach(attribute -> ids.add(attribute.id()));
            entity.relations().forEach(relation -> ids.add(relation.id()));
        }
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            if (!seen.add(id)) {
                issues.error(id, "Modelo", "El id interno '" + id + "' se repite: cada elemento necesita el suyo");
            }
        }
    }

    // ── Nombres de clases ────────────────────────────────────────────

    /**
     * Revisa que ningún nombre de clase se repita entre superclases y
     * entidades, sin distinguir mayúsculas.
     *
     * @param model  modelo
     * @param issues donde se anotan los problemas
     */
    private static void checkClassNames(final DomainModel model, final Issues issues) {
        Map<String, String> seen = new HashMap<>();
        for (Superclass superclass : model.superclasses()) {
            claimClassName(seen, superclass.name(), "la superclase " + superclass.name(), superclass.id(),
                    label(superclass), issues);
        }
        for (Entity entity : model.entities()) {
            claimClassName(seen, entity.name(), entity.name(), entity.id(), label(entity), issues);
        }
    }

    /**
     * Anota un nombre de clase y avisa si ya estaba.
     *
     * @param seen        nombres usados, con quién los usa
     * @param name        nombre nuevo
     * @param description quién lo usa, para el mensaje
     * @param elementId   elemento afectado
     * @param location    ubicación legible
     * @param issues      donde se anotan los problemas
     */
    private static void claimClassName(final Map<String, String> seen, final String name, final String description,
                                       final String elementId, final String location, final Issues issues) {
        if (name.isEmpty()) {
            return;
        }
        String first = seen.putIfAbsent(name.toLowerCase(Locale.ROOT), description);
        if (first != null) {
            issues.error(elementId, location, "Tiene el nombre «" + name + "», igual que " + first);
        }
    }

    /**
     * Revisa el nombre de una entidad o superclase.
     *
     * @param elementId elemento afectado
     * @param name      nombre
     * @param location  ubicación legible
     * @param kind      qué es, para el mensaje: {@code la entidad} o {@code la superclase}
     * @param example   nombre de ejemplo, para el mensaje
     * @param issues    donde se anotan los problemas
     */
    private static void checkClassName(final String elementId, final String name, final String location,
                                       final String kind, final String example, final Issues issues) {
        if (name.isEmpty()) {
            issues.error(elementId, location, "El nombre de " + kind + " es obligatorio");
        } else if (!ENTITY_NAME.matcher(name).matches()) {
            issues.error(elementId, location, "«" + name + "» debe estar en PascalCase sin tildes ni espacios, "
                    + "por ejemplo " + example);
        } else if (ReservedWords.reservedClassName(name)) {
            issues.error(elementId, location, "«" + name + "» choca con una clase que usa el código generado "
                    + "(Java, JPA o Spring); use otro nombre");
        }
    }

    // ── Superclases ──────────────────────────────────────────────────

    /**
     * Revisa una superclase: nombre, padre, ciclos, atributos y choques con
     * lo heredado.
     *
     * @param model      modelo
     * @param superclass superclase
     * @param issues     donde se anotan los problemas
     */
    private static void checkSuperclass(final DomainModel model, final Superclass superclass, final Issues issues) {
        String location = label(superclass);
        checkClassName(superclass.id(), superclass.name(), location, "la superclase", "Auditable", issues);
        boolean cyclic = false;
        if (superclass.parentId() != null) {
            if (model.findSuperclass(superclass.parentId()).isEmpty()) {
                issues.error(superclass.id(), location, "La superclase padre no existe: elija una del modelo");
            } else if (formsCycle(model, superclass)) {
                cyclic = true;
                issues.error(superclass.id(), location, "La herencia forma un ciclo: " + location
                        + " termina heredando de sí misma");
            }
        }
        Map<String, String> members = auditMembers(model.audit());
        Map<String, String> columns = baseColumns(model.audit());
        if (!cyclic) {
            inherit(model, superclass.parentId(), members, columns);
        }
        String where = "las tablas que heredan de " + location;
        for (Attribute attribute : superclass.attributes()) {
            String at = member(location, attribute.name());
            checkAttribute(attribute, at, issues);
            addMember(members, attribute.name(), "el atributo " + attribute.name(), attribute.id(), at, location,
                    issues);
            addColumn(columns, attribute.effectiveColumnName(), "el atributo " + attribute.name(), attribute.id(),
                    at, where, issues);
        }
    }

    /**
     * Indica si, subiendo por los padres, se vuelve a la superclase inicial.
     *
     * @param model modelo
     * @param start superclase inicial
     * @return {@code true} si forma parte de un ciclo
     */
    private static boolean formsCycle(final DomainModel model, final Superclass start) {
        String current = start.parentId();
        for (int steps = 0; current != null && steps <= model.superclasses().size(); steps++) {
            if (current.equals(start.id())) {
                return true;
            }
            current = model.findSuperclass(current).map(Superclass::parentId).orElse(null);
        }
        return false;
    }

    /**
     * Avisa de las superclases de las que no hereda ninguna entidad, ni
     * directa ni indirectamente.
     *
     * @param model  modelo
     * @param issues donde se anotan los problemas
     */
    private static void checkUnusedSuperclasses(final DomainModel model, final Issues issues) {
        Set<String> used = new HashSet<>();
        for (Entity entity : model.entities()) {
            model.lineage(entity.superclassId()).forEach(superclass -> used.add(superclass.id()));
        }
        for (Superclass superclass : model.superclasses()) {
            if (!used.contains(superclass.id())) {
                issues.warning(superclass.id(), label(superclass), "Ninguna entidad hereda de ella: no aparecerá "
                        + "en el código generado");
            }
        }
    }

    /**
     * Anota los nombres y columnas heredados de una cadena de superclases,
     * desde la raíz, sin informar choques: esos se informan donde se declaran.
     *
     * @param model        modelo
     * @param superclassId superclase más cercana, o {@code null}
     * @param members      donde se anotan los nombres heredados
     * @param columns      donde se anotan las columnas heredadas
     */
    private static void inherit(final DomainModel model, final String superclassId, final Map<String, String> members,
                                final Map<String, String> columns) {
        for (Superclass ancestor : model.lineage(superclassId).reversed()) {
            for (Attribute attribute : ancestor.attributes()) {
                String description = "el atributo " + attribute.name() + " de " + label(ancestor);
                if (!attribute.name().isEmpty()) {
                    members.putIfAbsent(attribute.name(), description);
                }
                String column = attribute.effectiveColumnName();
                if (!column.isEmpty()) {
                    columns.putIfAbsent(column.toLowerCase(Locale.ROOT), description);
                }
            }
        }
    }

    /**
     * Nombres que ocupa la auditoría.
     *
     * @param audit campos de auditoría
     * @return mapa mutable de nombre a quién lo usa
     */
    private static Map<String, String> auditMembers(final AuditFields audit) {
        Map<String, String> members = new HashMap<>();
        if (audit.createdAt()) {
            members.put(AuditFields.CREATED_AT, AUDIT);
        }
        if (audit.updatedAt()) {
            members.put(AuditFields.UPDATED_AT, AUDIT);
        }
        return members;
    }

    /**
     * Columnas que toda tabla tiene: el id y la auditoría.
     *
     * @param audit campos de auditoría
     * @return mapa mutable de columna a quién la usa
     */
    private static Map<String, String> baseColumns(final AuditFields audit) {
        Map<String, String> columns = new HashMap<>();
        columns.put(ID, "el id");
        auditMembers(audit).keySet().forEach(name -> columns.put(ModelNaming.snakeCase(name), AUDIT));
        return columns;
    }

    // ── Entidades ────────────────────────────────────────────────────

    /**
     * Revisa que tablas y rutas de las entidades no se repitan.
     *
     * @param entities entidades del modelo
     * @param issues   donde se anotan los problemas
     */
    private static void checkEntityUniqueness(final List<Entity> entities, final Issues issues) {
        checkUnique(entities, Entity::effectiveTableName, "la tabla", issues);
        checkUnique(entities, Entity::effectiveResourcePath, "la ruta", issues);
    }

    /**
     * Revisa que un dato de las entidades no se repita, sin distinguir mayúsculas.
     *
     * @param entities entidades del modelo
     * @param key      dato a comparar
     * @param what     qué dato es, para el mensaje
     * @param issues   donde se anotan los problemas
     */
    private static void checkUnique(final List<Entity> entities, final Function<Entity, String> key,
                                    final String what, final Issues issues) {
        Map<String, Entity> seen = new HashMap<>();
        for (Entity entity : entities) {
            if (entity.name().isEmpty()) {
                continue;
            }
            String value = key.apply(entity);
            Entity first = seen.putIfAbsent(value.toLowerCase(Locale.ROOT), entity);
            if (first != null) {
                issues.error(entity.id(), label(entity), "Tiene " + what + " «" + value + "», igual que "
                        + label(first));
            }
        }
    }

    /**
     * Revisa una entidad, su superclase, sus atributos, sus relaciones y los
     * inversos que le llegan.
     *
     * @param model       modelo
     * @param entity      entidad
     * @param derivations derivaciones del modelo
     * @param issues      donde se anotan los problemas
     */
    private static void checkEntity(final DomainModel model, final Entity entity, final ModelDerivations derivations,
                                    final Issues issues) {
        String location = label(entity);
        checkClassName(entity.id(), entity.name(), location, "la entidad", "DetallePedido", issues);
        checkSqlName(entity.id(), location, "La tabla", entity.effectiveTableName(), issues);
        if (!entity.name().isEmpty() && !RESOURCE_PATH.matcher(entity.effectiveResourcePath()).matches()) {
            issues.error(entity.id(), location, "La ruta «" + entity.effectiveResourcePath() + "» no es válida: "
                    + "use minúsculas, números y guiones, empezando por /, por ejemplo /detalle-pedidos");
        }
        if (model.idSettings().generation().needsSequences()) {
            checkSqlName(entity.id(), location, "La secuencia", entity.effectiveSequenceName(), issues);
        }
        if (entity.superclassId() != null) {
            if (model.findSuperclass(entity.superclassId()).isEmpty()) {
                issues.error(entity.id(), location, "La superclase elegida no existe: elija una del modelo");
            }
        } else if (model.idSettings().placement() == IdPlacement.ROOT_SUPERCLASS) {
            issues.error(entity.id(), location, "Debe heredar de una superclase, porque el id va en la superclase "
                    + "raíz");
        }
        if (entity.attributes().isEmpty() && entity.relations().isEmpty() && entity.superclassId() == null) {
            issues.warning(entity.id(), location, "No tiene atributos ni relaciones: solo tendrá id");
        }

        Map<String, String> members = auditMembers(model.audit());
        inherit(model, entity.superclassId(), members, new HashMap<>());
        for (Attribute attribute : entity.attributes()) {
            String at = member(entity, attribute.name());
            checkAttribute(attribute, at, issues);
            addMember(members, attribute.name(), "el atributo " + attribute.name(), attribute.id(), at, location,
                    issues);
        }
        for (Relation relation : entity.relations()) {
            String at = member(entity, relation.name());
            checkRelation(model, entity, relation, at, issues);
            addMember(members, relation.name(), "la relación " + relation.name(), relation.id(), at, location,
                    issues);
        }
        for (InverseRelation inverse : derivations.inversesOf(entity.id())) {
            Entity source = model.findEntity(inverse.sourceEntityId()).orElseThrow();
            Relation relation = source.findRelation(inverse.relationId()).orElseThrow();
            String at = member(source, relation.name());
            checkMemberName(inverse.name(), relation.id(), at, "El inverso", issues);
            addMember(members, inverse.name(), "el inverso de " + at, relation.id(), at, location, issues);
        }
    }

    /**
     * Anota un nombre en una clase y avisa si ya estaba.
     *
     * @param members     nombres usados, con quién los usa
     * @param name        nombre nuevo
     * @param description quién lo usa, para el mensaje
     * @param elementId   elemento afectado
     * @param location    ubicación legible
     * @param owner       clase donde se anota, para el mensaje
     * @param issues      donde se anotan los problemas
     */
    private static void addMember(final Map<String, String> members, final String name, final String description,
                                  final String elementId, final String location, final String owner,
                                  final Issues issues) {
        if (name.isEmpty() || ID.equals(name)) {
            return;
        }
        String previous = members.putIfAbsent(name, description);
        if (previous != null) {
            issues.error(elementId, location, "«" + name + "» se repite en " + owner + ": ya lo usa " + previous);
        }
    }

    // ── Atributos ────────────────────────────────────────────────────

    /**
     * Revisa un atributo: nombre, tipo, restricciones y columna.
     *
     * @param attribute atributo
     * @param location  ubicación legible
     * @param issues    donde se anotan los problemas
     */
    private static void checkAttribute(final Attribute attribute, final String location, final Issues issues) {
        checkMemberName(attribute.name(), attribute.id(), location, "El nombre", issues);
        if (attribute.type().usesEnum()) {
            issues.error(attribute.id(), location, "Los enums todavía no están disponibles: use un tipo simple");
            return;
        }
        checkConstraints(attribute, attribute.type().scalar(), location, issues);
        checkSqlName(attribute.id(), location, "La columna", attribute.effectiveColumnName(), issues);
    }

    /**
     * Revisa el nombre de un atributo, relación o inverso.
     *
     * @param name      nombre
     * @param elementId elemento afectado
     * @param location  ubicación legible
     * @param subject   qué nombre es, para el mensaje
     * @param issues    donde se anotan los problemas
     */
    private static void checkMemberName(final String name, final String elementId, final String location,
                                        final String subject, final Issues issues) {
        if (name.isEmpty()) {
            issues.error(elementId, location, subject + " es obligatorio");
        } else if (!MEMBER_NAME.matcher(name).matches()) {
            issues.error(elementId, location, subject + " «" + name + "» debe estar en camelCase sin tildes ni "
                    + "espacios, por ejemplo fechaNacimiento");
        } else if (ReservedWords.javaKeyword(name)) {
            issues.error(elementId, location, "«" + name + "» es una palabra reservada de Java");
        } else if (ID.equals(name)) {
            issues.error(elementId, location, "«id» lo agrega el modelo: use otro nombre");
        }
    }

    /**
     * Revisa que las restricciones sean compatibles con el tipo y coherentes entre sí.
     *
     * @param attribute atributo
     * @param type      tipo simple del atributo
     * @param location  ubicación legible
     * @param issues    donde se anotan los problemas
     */
    private static void checkConstraints(final Attribute attribute, final ScalarType type, final String location,
                                         final Issues issues) {
        AttributeConstraints c = attribute.constraints();
        String id = attribute.id();
        boolean textRules = c.minLength() != null || c.maxLength() != null || c.pattern() != null || c.email();
        if (textRules && !type.acceptsTextRules()) {
            issues.error(id, location, "La longitud, el patrón y el email solo aplican a textos, no a "
                    + type.getJavaType());
        }
        if ((c.min() != null || c.max() != null) && !type.acceptsRange()) {
            issues.error(id, location, "El mínimo y el máximo solo aplican a números, no a " + type.getJavaType());
        }
        if ((c.integerDigits() != null || c.fractionDigits() != null) && !type.acceptsDigits()) {
            issues.error(id, location, "Los dígitos enteros y decimales solo aplican a BigDecimal");
        }
        if (c.temporal() != null && !type.acceptsTemporalRule()) {
            issues.error(id, location, "Pasado o futuro solo aplica a fechas e instantes, no a " + type.getJavaType());
        }
        checkCoherence(c, type, id, location, issues);
    }

    /**
     * Revisa que los valores de las restricciones tengan sentido.
     *
     * @param c        restricciones
     * @param type     tipo simple del atributo
     * @param id       elemento afectado
     * @param location ubicación legible
     * @param issues   donde se anotan los problemas
     */
    private static void checkCoherence(final AttributeConstraints c, final ScalarType type, final String id,
                                       final String location, final Issues issues) {
        if (c.minLength() != null && c.minLength() < 0) {
            issues.error(id, location, "La longitud mínima no puede ser negativa");
        }
        if (c.maxLength() != null && c.maxLength() < 1) {
            issues.error(id, location, "La longitud máxima debe ser al menos 1");
        }
        if (c.minLength() != null && c.maxLength() != null && c.minLength() > c.maxLength()) {
            issues.error(id, location, "La longitud mínima (" + c.minLength() + ") es mayor que la máxima ("
                    + c.maxLength() + ")");
        }
        if (c.min() != null && c.max() != null && c.min().compareTo(c.max()) > 0) {
            issues.error(id, location, "El mínimo (" + c.min().toPlainString() + ") es mayor que el máximo ("
                    + c.max().toPlainString() + ")");
        }
        if (type.getFamily() == ScalarType.Family.INTEGRAL && (hasDecimals(c.min()) || hasDecimals(c.max()))) {
            issues.error(id, location, "El mínimo y el máximo de un entero no llevan decimales");
        }
        if (c.integerDigits() != null && c.integerDigits() < 1) {
            issues.error(id, location, "Los dígitos enteros deben ser al menos 1");
        }
        if (c.fractionDigits() != null && c.fractionDigits() < 0) {
            issues.error(id, location, "Los dígitos decimales no pueden ser negativos");
        }
        if (c.pattern() != null) {
            try {
                Pattern.compile(c.pattern());
            } catch (PatternSyntaxException e) {
                issues.error(id, location, "El patrón no es una expresión regular válida: " + e.getDescription());
            }
        }
    }

    /**
     * Indica si un número tiene parte decimal distinta de cero.
     *
     * @param value número, o {@code null}
     * @return {@code true} si existe y tiene decimales
     */
    private static boolean hasDecimals(final BigDecimal value) {
        return value != null && value.stripTrailingZeros().scale() > 0;
    }

    // ── Relaciones ───────────────────────────────────────────────────

    /**
     * Revisa una relación: nombre, destino, obligatoriedad e inverso.
     *
     * @param model    modelo
     * @param entity   entidad que la declara
     * @param relation relación
     * @param location ubicación legible
     * @param issues   donde se anotan los problemas
     */
    private static void checkRelation(final DomainModel model, final Entity entity, final Relation relation,
                                      final String location, final Issues issues) {
        checkMemberName(relation.name(), relation.id(), location, "El nombre", issues);
        Optional<Entity> target = model.findEntity(relation.targetEntityId());
        if (target.isEmpty()) {
            issues.error(relation.id(), location, "La entidad destino no existe: elija una del modelo");
        }
        if (relation.required() && !relation.cardinality().allowsRequired()) {
            issues.error(relation.id(), location, "Una relación " + relation.cardinality().getLabel()
                    + " no puede ser obligatoria");
        }
        boolean self = target.isPresent() && target.get().id().equals(entity.id());
        if (self && relation.bidirectional() && relation.inverseName().equals(relation.name())) {
            issues.error(relation.id(), location, "El inverso no puede llamarse igual que la relación en la misma "
                    + "entidad");
        }
        if (relation.joinTable() != null && relation.cardinality() != Cardinality.MANY_TO_MANY) {
            issues.warning(relation.id(), location, "La tabla intermedia solo se usa en relaciones N‑N; se ignora");
        }
    }

    // ── Tablas y columnas ────────────────────────────────────────────

    /**
     * Revisa que ninguna columna se repita en la tabla de una entidad: id,
     * auditoría, atributos heredados, atributos propios y claves foráneas que
     * caen en ella.
     *
     * @param model       modelo
     * @param entity      entidad
     * @param derivations derivaciones del modelo
     * @param issues      donde se anotan los problemas
     */
    private static void checkColumns(final DomainModel model, final Entity entity, final ModelDerivations derivations,
                                     final Issues issues) {
        String where = "la tabla " + entity.effectiveTableName();
        Map<String, String> columns = baseColumns(model.audit());
        inherit(model, entity.superclassId(), new HashMap<>(), columns);
        for (Attribute attribute : entity.attributes()) {
            addColumn(columns, attribute.effectiveColumnName(), "el atributo " + attribute.name(), attribute.id(),
                    member(entity, attribute.name()), where, issues);
        }
        for (Entity declaring : model.entities()) {
            for (Relation relation : declaring.relations()) {
                Optional<RelationMapping> mapping = derivations.mappingOf(relation.id());
                if (mapping.isEmpty() || mapping.get().usesJoinTable()
                        || !mapping.get().ownerEntityId().equals(entity.id())) {
                    continue;
                }
                String at = member(declaring, relation.name());
                checkSqlName(relation.id(), at, "La columna", mapping.get().joinColumn(), issues);
                addColumn(columns, mapping.get().joinColumn(), "la relación " + at, relation.id(), at, where, issues);
            }
        }
    }

    /**
     * Anota una columna y avisa si ya estaba.
     *
     * @param columns     columnas usadas, con quién las usa
     * @param column      columna nueva
     * @param description quién la usa, para el mensaje
     * @param elementId   elemento afectado
     * @param location    ubicación legible
     * @param where       tabla o tablas afectadas, para el mensaje
     * @param issues      donde se anotan los problemas
     */
    private static void addColumn(final Map<String, String> columns, final String column, final String description,
                                  final String elementId, final String location, final String where,
                                  final Issues issues) {
        if (column.isEmpty()) {
            return;
        }
        String previous = columns.putIfAbsent(column.toLowerCase(Locale.ROOT), description);
        if (previous != null) {
            issues.error(elementId, location, "La columna «" + column + "» se repite en " + where + ": ya la usa "
                    + previous);
        }
    }

    /**
     * Revisa las tablas intermedias de las relaciones N‑N: nombres válidos,
     * columnas distintas y sin chocar con otras tablas.
     *
     * @param model       modelo
     * @param derivations derivaciones del modelo
     * @param issues      donde se anotan los problemas
     */
    private static void checkJoinTables(final DomainModel model, final ModelDerivations derivations,
                                        final Issues issues) {
        Map<String, String> tables = new HashMap<>();
        model.entities().forEach(entity ->
                tables.putIfAbsent(entity.effectiveTableName().toLowerCase(Locale.ROOT), "la entidad " + label(entity)));
        for (Entity declaring : model.entities()) {
            for (Relation relation : declaring.relations()) {
                Optional<RelationMapping> found = derivations.mappingOf(relation.id());
                if (found.isEmpty() || !found.get().usesJoinTable()) {
                    continue;
                }
                RelationMapping mapping = found.get();
                String at = member(declaring, relation.name());
                checkSqlName(relation.id(), at, "La tabla intermedia", mapping.joinTable(), issues);
                checkSqlName(relation.id(), at, "La columna", mapping.joinColumn(), issues);
                checkSqlName(relation.id(), at, "La columna", mapping.inverseJoinColumn(), issues);
                if (mapping.joinColumn().equalsIgnoreCase(mapping.inverseJoinColumn())) {
                    issues.error(relation.id(), at, "Las dos columnas de la tabla intermedia se llaman igual («"
                            + mapping.joinColumn() + "»)");
                }
                String previous = tables.putIfAbsent(mapping.joinTable().toLowerCase(Locale.ROOT),
                        "la relación " + at);
                if (previous != null) {
                    issues.error(relation.id(), at, "La tabla intermedia «" + mapping.joinTable() + "» ya la usa "
                            + previous);
                }
            }
        }
    }

    /**
     * Revisa un nombre de tabla, columna o secuencia. Los vacíos se omiten:
     * vienen de un nombre vacío que ya se informó.
     *
     * @param elementId elemento afectado
     * @param location  ubicación legible
     * @param subject   qué nombre es, para el mensaje
     * @param value     nombre en la base
     * @param issues    donde se anotan los problemas
     */
    private static void checkSqlName(final String elementId, final String location, final String subject,
                                     final String value, final Issues issues) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (value.length() > MAX_SQL_NAME) {
            issues.error(elementId, location, subject + " «" + value + "» supera " + MAX_SQL_NAME + " caracteres");
        } else if (!SQL_NAME.matcher(value).matches()) {
            issues.error(elementId, location, subject + " «" + value + "» debe usar minúsculas, números y guion "
                    + "bajo (snake_case)");
        } else if (ReservedWords.sqlReserved(value)) {
            issues.warning(elementId, location, subject + " «" + value + "» es palabra reservada en SQL; se "
                    + "recomienda otro nombre");
        }
    }

    // ── Ubicaciones ──────────────────────────────────────────────────

    /**
     * Nombre legible de una entidad.
     *
     * @param entity entidad
     * @return su nombre, o un texto que indica que no tiene
     */
    private static String label(final Entity entity) {
        return entity.name().isEmpty() ? "(entidad sin nombre)" : entity.name();
    }

    /**
     * Nombre legible de una superclase.
     *
     * @param superclass superclase
     * @return su nombre, o un texto que indica que no tiene
     */
    private static String label(final Superclass superclass) {
        return superclass.name().isEmpty() ? "(superclase sin nombre)" : superclass.name();
    }

    /**
     * Ubicación legible de un atributo o relación de una entidad.
     *
     * @param entity entidad
     * @param name   nombre del atributo o relación
     * @return por ejemplo {@code Pedido › cliente}
     */
    private static String member(final Entity entity, final String name) {
        return member(label(entity), name);
    }

    /**
     * Ubicación legible de un atributo o relación.
     *
     * @param owner nombre legible de la clase
     * @param name  nombre del atributo o relación
     * @return por ejemplo {@code Auditable › activo}
     */
    private static String member(final String owner, final String name) {
        return owner + ARROW + nameOrPlaceholder(name);
    }

    /**
     * Nombre legible, o un texto que indica que no tiene.
     *
     * @param name nombre
     * @return el nombre o {@code (sin nombre)}
     */
    private static String nameOrPlaceholder(final String name) {
        return name.isEmpty() ? "(sin nombre)" : name;
    }

    /**
     * Problemas encontrados durante una validación.
     */
    private static final class Issues {

        /** Problemas, en orden. */
        private final List<ModelIssue> list = new ArrayList<>();

        /**
         * Anota un error.
         *
         * @param elementId elemento afectado, o {@code null}
         * @param location  ubicación legible
         * @param message   explicación
         */
        void error(final String elementId, final String location, final String message) {
            list.add(new ModelIssue(IssueSeverity.ERROR, elementId, location, message));
        }

        /**
         * Anota un aviso.
         *
         * @param elementId elemento afectado, o {@code null}
         * @param location  ubicación legible
         * @param message   explicación
         */
        void warning(final String elementId, final String location, final String message) {
            list.add(new ModelIssue(IssueSeverity.WARNING, elementId, location, message));
        }

        /**
         * Devuelve los problemas anotados.
         *
         * @return lista inmutable
         */
        List<ModelIssue> list() {
            return List.copyOf(list);
        }
    }
}