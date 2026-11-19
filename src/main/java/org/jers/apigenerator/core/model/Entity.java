package org.jers.apigenerator.core.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Entidad del modelo. El id y la auditoría los agrega el modelo según
 * {@link IdSettings} y {@link AuditFields}; aquí van solo los atributos y
 * relaciones propios.
 *
 * @param id           id interno estable; no cambia al renombrar
 * @param name         nombre en PascalCase, por ejemplo {@code DetallePedido}
 * @param tableName    tabla escrita por el usuario, o {@code null} para derivarla
 * @param resourcePath ruta REST escrita por el usuario, o {@code null} para derivarla
 * @param sequenceName secuencia escrita por el usuario, o {@code null} para derivarla
 * @param superclassId id interno de la superclase de la que hereda, o {@code null}
 * @param attributes   atributos propios, en orden
 * @param relations    relaciones que declara esta entidad, en orden
 */
public record Entity(
        String id,
        String name,
        String tableName,
        String resourcePath,
        String sequenceName,
        String superclassId,
        List<Attribute> attributes,
        List<Relation> relations) {

    /**
     * Normaliza los textos y protege las listas.
     *
     * @throws org.jers.apigenerator.core.common.DomainValidationException si falta el id interno
     * @throws NullPointerException si algún atributo o relación es {@code null}
     */
    public Entity {
        id = ModelText.internalId(id, "La entidad '" + name + "'");
        name = ModelText.required(name);
        tableName = ModelText.optional(tableName);
        resourcePath = ModelText.optional(resourcePath);
        sequenceName = ModelText.optional(sequenceName);
        superclassId = ModelText.optional(superclassId);
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
        relations = relations == null ? List.of() : List.copyOf(relations);
    }

    /**
     * Crea una entidad sin superclase.
     *
     * @param id           id interno
     * @param name         nombre en PascalCase
     * @param tableName    tabla escrita, o {@code null}
     * @param resourcePath ruta escrita, o {@code null}
     * @param sequenceName secuencia escrita, o {@code null}
     * @param attributes   atributos propios
     * @param relations    relaciones que declara
     */
    public Entity(final String id, final String name, final String tableName, final String resourcePath,
                  final String sequenceName, final List<Attribute> attributes, final List<Relation> relations) {
        this(id, name, tableName, resourcePath, sequenceName, null, attributes, relations);
    }

    /**
     * Busca una relación declarada por esta entidad.
     *
     * @param relationId id interno de la relación
     * @return la relación, o vacío si no existe
     */
    public Optional<Relation> findRelation(final String relationId) {
        Objects.requireNonNull(relationId, "relationId es obligatorio");
        return relations.stream().filter(relation -> relation.id().equals(relationId)).findFirst();
    }

    /**
     * Tabla que tendrá en la base.
     *
     * @return la escrita por el usuario o, si no hay, el nombre en snake_case
     */
    public String effectiveTableName() {
        return tableName != null ? tableName : ModelNaming.snakeCase(name);
    }

    /**
     * Ruta REST de su controlador.
     *
     * @return la escrita por el usuario o, si no hay, el nombre en kebab-case y plural
     */
    public String effectiveResourcePath() {
        return resourcePath != null ? resourcePath : ModelNaming.resourcePath(name);
    }

    /**
     * Secuencia de su id, usada con {@link IdGeneration#SEQUENCE}.
     *
     * @return la escrita por el usuario o, si no hay, la tabla con {@code _seq}
     */
    public String effectiveSequenceName() {
        return sequenceName != null ? sequenceName : effectiveTableName() + ModelNaming.SEQUENCE_SUFFIX;
    }
}