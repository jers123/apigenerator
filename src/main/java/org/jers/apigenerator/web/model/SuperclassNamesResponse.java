package org.jers.apigenerator.web.model;

import org.jers.apigenerator.core.model.Attribute;
import org.jers.apigenerator.core.model.Superclass;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nombres efectivos de una superclase, para mostrarlos en la pantalla junto
 * a los campos que el usuario dejó vacíos.
 *
 * @param superclassId id interno de la superclase
 * @param columns      columna efectiva de cada atributo, por id interno del atributo
 */
public record SuperclassNamesResponse(String superclassId, Map<String, String> columns) {

	/**
	 * Calcula los nombres efectivos de una superclase.
	 *
	 * @param superclass superclase del modelo
	 * @return nombres para la API, con las columnas en el orden de los atributos
	 */
	public static SuperclassNamesResponse from(final Superclass superclass) {
		Map<String, String> columns = new LinkedHashMap<>();
		for (Attribute attribute : superclass.attributes()) {
			columns.put(attribute.id(), attribute.effectiveColumnName());
		}
		return new SuperclassNamesResponse(superclass.id(), Collections.unmodifiableMap(columns));
	}
}