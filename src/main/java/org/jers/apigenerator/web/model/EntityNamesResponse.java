package org.jers.apigenerator.web.model;

import org.jers.apigenerator.core.model.Attribute;
import org.jers.apigenerator.core.model.Entity;
import org.jers.apigenerator.core.model.InverseRelation;
import org.jers.apigenerator.core.model.ModelDerivations;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nombres efectivos de una entidad, para mostrarlos en la pantalla junto a
 * los campos que el usuario dejó vacíos.
 *
 * @param entityId     id interno de la entidad
 * @param tableName    tabla efectiva
 * @param resourcePath ruta REST efectiva
 * @param sequenceName secuencia efectiva; solo se usa con la estrategia SEQUENCE
 * @param columns      columna efectiva de cada atributo, por id interno del atributo
 * @param inverses     relaciones inversas que llegan a esta entidad desde otras
 */
public record EntityNamesResponse(
		String entityId,
		String tableName,
		String resourcePath,
		String sequenceName,
		Map<String, String> columns,
		List<InverseRelation> inverses) {

	/**
	 * Calcula los nombres efectivos de una entidad.
	 *
	 * @param entity      entidad del modelo
	 * @param derivations derivaciones del modelo
	 * @return nombres para la API, con las columnas en el orden de los atributos
	 */
	public static EntityNamesResponse from(final Entity entity, final ModelDerivations derivations) {
		Map<String, String> columns = new LinkedHashMap<>();
		for (Attribute attribute : entity.attributes()) {
			columns.put(attribute.id(), attribute.effectiveColumnName());
		}
		return new EntityNamesResponse(
				entity.id(),
				entity.effectiveTableName(),
				entity.effectiveResourcePath(),
				entity.effectiveSequenceName(),
				Collections.unmodifiableMap(columns),
				derivations.inversesOf(entity.id()));
	}
}