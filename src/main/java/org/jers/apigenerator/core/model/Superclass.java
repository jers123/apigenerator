package org.jers.apigenerator.core.model;

import java.util.List;

/**
 * Superclase abstracta con {@code @MappedSuperclass}: aporta atributos a las
 * entidades que heredan de ella y no tiene tabla propia. Puede heredar de
 * otra superclase, formando un árbol de cualquier profundidad.
 *
 * @param id         id interno estable; no cambia al renombrar
 * @param name       nombre en PascalCase, por ejemplo {@code Auditable}
 * @param parentId   id interno de la superclase de la que hereda, o
 *                   {@code null} si es raíz
 * @param attributes atributos que aporta, en orden
 */
public record Superclass(String id, String name, String parentId, List<Attribute> attributes) {

	/**
	 * Normaliza los textos y protege la lista.
	 *
	 * @throws org.jers.apigenerator.core.common.DomainValidationException si falta el id interno
	 * @throws NullPointerException si algún atributo es {@code null}
	 */
	public Superclass {
		id = ModelText.internalId(id, "La superclase '" + name + "'");
		name = ModelText.required(name);
		parentId = ModelText.optional(parentId);
		attributes = attributes == null ? List.of() : List.copyOf(attributes);
	}
}