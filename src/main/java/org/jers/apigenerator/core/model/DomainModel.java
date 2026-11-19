package org.jers.apigenerator.core.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Modelo de dominio completo del proyecto.
 *
 * @param idSettings   tipo, generación y ubicación del id de todas las entidades
 * @param audit        campos de auditoría; van donde va el id
 * @param superclasses superclases con {@code @MappedSuperclass}, en el orden en que se muestran
 * @param entities     entidades, en el orden en que se muestran
 */
public record DomainModel(
        IdSettings idSettings,
        AuditFields audit,
        List<Superclass> superclasses,
        List<Entity> entities) {

    /**
     * Completa los valores por defecto y protege las listas.
     *
     * @throws NullPointerException si alguna superclase o entidad es {@code null}
     */
    public DomainModel {
        idSettings = idSettings == null ? IdSettings.defaults() : idSettings;
        audit = audit == null ? AuditFields.none() : audit;
        superclasses = superclasses == null ? List.of() : List.copyOf(superclasses);
        entities = entities == null ? List.of() : List.copyOf(entities);
    }

    /**
     * Crea un modelo sin superclases.
     *
     * @param idSettings tipo, generación y ubicación del id
     * @param audit      campos de auditoría
     * @param entities   entidades
     */
    public DomainModel(final IdSettings idSettings, final AuditFields audit, final List<Entity> entities) {
        this(idSettings, audit, List.of(), entities);
    }

    /**
     * Modelo vacío: el proyecto se genera sin CRUD.
     *
     * @return modelo sin superclases ni entidades, con id por defecto y sin auditoría
     */
    public static DomainModel empty() {
        return new DomainModel(IdSettings.defaults(), AuditFields.none(), List.of(), List.of());
    }

    /**
     * Busca una entidad por su id interno.
     *
     * @param entityId id interno
     * @return la entidad, o vacío si no existe
     */
    public Optional<Entity> findEntity(final String entityId) {
        Objects.requireNonNull(entityId, "entityId es obligatorio");
        return entities.stream().filter(entity -> entity.id().equals(entityId)).findFirst();
    }

    /**
     * Busca una superclase por su id interno.
     *
     * @param superclassId id interno
     * @return la superclase, o vacío si no existe
     */
    public Optional<Superclass> findSuperclass(final String superclassId) {
        Objects.requireNonNull(superclassId, "superclassId es obligatorio");
        return superclasses.stream().filter(superclass -> superclass.id().equals(superclassId)).findFirst();
    }

    /**
     * Cadena de herencia desde una superclase hasta su raíz. Es tolerante:
     * se detiene en un padre que no existe o al repetir una superclase, por
     * ejemplo en un ciclo; esos errores los informa el validador.
     *
     * @param superclassId id interno de la superclase inicial, o {@code null}
     * @return la superclase, su padre, el padre de este… hasta la raíz;
     *         vacía si el id es {@code null} o no existe
     */
    public List<Superclass> lineage(final String superclassId) {
        List<Superclass> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String current = superclassId;
        while (current != null && visited.add(current)) {
            Optional<Superclass> found = findSuperclass(current);
            if (found.isEmpty()) {
                break;
            }
            chain.add(found.get());
            current = found.get().parentId();
        }
        return List.copyOf(chain);
    }
}