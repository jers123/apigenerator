package org.jers.apigenerator.core.model;

import java.util.Objects;

/**
 * Atributo simple de una entidad o un campo común.
 *
 * @param id          id interno estable; no cambia al renombrar
 * @param name        nombre en camelCase, por ejemplo {@code fechaNacimiento}
 * @param columnName  columna escrita por el usuario, o {@code null} para derivarla
 * @param type        tipo del atributo
 * @param required    si es obligatorio ({@code NOT NULL} y {@code @NotNull})
 * @param unique      si su valor no se puede repetir
 * @param constraints restricciones; nunca {@code null}
 */
public record Attribute(
        String id,
        String name,
        String columnName,
        AttributeType type,
        boolean required,
        boolean unique,
        AttributeConstraints constraints) {

    /**
     * Normaliza los textos y completa las restricciones vacías.
     *
     * @throws org.jers.apigenerator.core.common.DomainValidationException si falta el id interno
     * @throws NullPointerException si falta el tipo
     */
    public Attribute {
        id = ModelText.internalId(id, "El atributo '" + name + "'");
        name = ModelText.required(name);
        columnName = ModelText.optional(columnName);
        Objects.requireNonNull(type, "type es obligatorio");
        constraints = constraints == null ? AttributeConstraints.none() : constraints;
    }

    /**
     * Columna que tendrá en la base.
     *
     * @return la escrita por el usuario o, si no hay, el nombre en snake_case
     */
    public String effectiveColumnName() {
        return columnName != null ? columnName : ModelNaming.snakeCase(name);
    }
}