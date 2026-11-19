package org.jers.apigenerator.core.project;

import org.jers.apigenerator.core.common.Preconditions;

/**
 * Grupos del catálogo que el modelo agrega solo al proyecto.
 *
 * @param persistenceGroupId grupo de persistencia (JPA o R2DBC), necesario
 *                           si hay entidades y base de datos de desarrollo
 * @param validationGroupId  grupo de Jakarta Validation, necesario si algún
 *                           atributo o relación es obligatorio o tiene restricciones
 */
public record ModelDependencyGroups(String persistenceGroupId, String validationGroupId) {

	/**
	 * Valida que ambos ids tengan formato de slug.
	 *
	 * @throws org.jers.apigenerator.core.common.DomainValidationException si alguno no es válido
	 */
	public ModelDependencyGroups {
		Preconditions.requireSlug(persistenceGroupId, "persistenceGroupId");
		Preconditions.requireSlug(validationGroupId, "validationGroupId");
	}

	/**
	 * Grupos del catálogo de fábrica.
	 *
	 * @return {@code spring-data} y {@code validation}
	 */
	public static ModelDependencyGroups defaults() {
		return new ModelDependencyGroups("spring-data", "validation");
	}
}