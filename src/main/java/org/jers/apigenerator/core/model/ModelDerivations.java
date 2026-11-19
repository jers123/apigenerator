package org.jers.apigenerator.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Lo que se deriva de un modelo y depende de varias entidades a la vez: los
 * lados inversos de las relaciones bidireccionales y el mapeo relacional de
 * cada relación.
 *
 * <p>Es tolerante: si una relación apunta a una entidad que no existe, la
 * omite; ese error lo informa el validador. Es inmutable y se calcula una
 * sola vez al construirse.</p>
 */
public final class ModelDerivations {

    /** Separador entre las tablas del nombre de una tabla intermedia. */
    private static final String JOIN_TABLE_SEPARATOR = "_";

    /** Lados inversos por id interno de la entidad destino. */
    private final Map<String, List<InverseRelation>> inverses;

    /** Mapeo relacional por id interno de la relación. */
    private final Map<String, RelationMapping> mappings;

    /**
     * Calcula las derivaciones de un modelo.
     *
     * @param model modelo, válido o no
     * @throws NullPointerException si el modelo es {@code null}
     */
    public ModelDerivations(final DomainModel model) {
        Objects.requireNonNull(model, "model es obligatorio");
        Map<String, List<InverseRelation>> inverseLists = new LinkedHashMap<>();
        Map<String, RelationMapping> relationMappings = new LinkedHashMap<>();
        for (Entity declaring : model.entities()) {
            for (Relation relation : declaring.relations()) {
                Optional<Entity> target = model.findEntity(relation.targetEntityId());
                if (target.isEmpty()) {
                    continue;
                }
                if (relation.bidirectional()) {
                    inverseLists.computeIfAbsent(target.get().id(), id -> new ArrayList<>())
                            .add(new InverseRelation(relation.inverseName(), declaring.id(), relation.id(),
                                    relation.cardinality().inverse()));
                }
                relationMappings.put(relation.id(), mapping(declaring, relation, target.get()));
            }
        }
        Map<String, List<InverseRelation>> frozen = new LinkedHashMap<>();
        inverseLists.forEach((entityId, list) -> frozen.put(entityId, List.copyOf(list)));
        this.inverses = Map.copyOf(frozen);
        this.mappings = Map.copyOf(relationMappings);
    }

    /**
     * Lados inversos que aparecen en una entidad.
     *
     * @param entityId id interno de la entidad destino
     * @return lista inmutable, vacía si no tiene
     */
    public List<InverseRelation> inversesOf(final String entityId) {
        return inverses.getOrDefault(entityId, List.of());
    }

    /**
     * Mapeo relacional de una relación.
     *
     * @param relationId id interno de la relación
     * @return el mapeo, o vacío si la relación no existe o su destino no existe
     */
    public Optional<RelationMapping> mappingOf(final String relationId) {
        return Optional.ofNullable(mappings.get(relationId));
    }

    /**
     * Calcula el mapeo de una relación según su cardinalidad. Los nombres
     * escritos por el usuario ganan sobre los derivados.
     *
     * @param declaring entidad que declara la relación
     * @param relation  relación
     * @param target    entidad destino
     * @return mapeo relacional
     */
    private static RelationMapping mapping(final Entity declaring, final Relation relation, final Entity target) {
        return switch (relation.cardinality()) {
            case MANY_TO_ONE, ONE_TO_ONE -> new RelationMapping(relation.id(), declaring.id(),
                    orDerived(relation.joinColumn(), ModelNaming.snakeCase(relation.name()) + ModelNaming.ID_SUFFIX),
                    null, null);
            case ONE_TO_MANY -> {
                String side = relation.bidirectional()
                        ? ModelNaming.snakeCase(relation.inverseName())
                        : declaring.effectiveTableName();
                yield new RelationMapping(relation.id(), target.id(),
                        orDerived(relation.joinColumn(), side + ModelNaming.ID_SUFFIX), null, null);
            }
            case MANY_TO_MANY -> {
                String owner = declaring.effectiveTableName();
                String other = declaring.id().equals(target.id())
                        ? ModelNaming.snakeCase(relation.name())
                        : target.effectiveTableName();
                yield new RelationMapping(relation.id(), declaring.id(),
                        orDerived(relation.joinColumn(), owner + ModelNaming.ID_SUFFIX),
                        orDerived(relation.joinTable(), owner + JOIN_TABLE_SEPARATOR + other),
                        other + ModelNaming.ID_SUFFIX);
            }
        };
    }

    /**
     * Elige el nombre escrito por el usuario o, si no hay, el derivado.
     *
     * @param written escrito por el usuario, o {@code null}
     * @param derived calculado
     * @return el que corresponde
     */
    private static String orDerived(final String written, final String derived) {
        return written != null ? written : derived;
    }
}