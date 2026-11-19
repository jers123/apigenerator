package org.jers.apigenerator.application.model;

import org.jers.apigenerator.core.model.DomainModel;
import org.jers.apigenerator.core.model.ModelDerivations;
import org.jers.apigenerator.core.model.ModelValidation;
import org.jers.apigenerator.core.model.ModelValidator;
import org.jers.apigenerator.core.project.ModelDependencyGroups;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Casos de uso del modelo de dominio: validarlo, calcular sus derivaciones e
 * informar qué grupos del catálogo agrega al proyecto.
 */
@Service
public class ModelService {

	/** Validador del núcleo; no tiene estado y se reutiliza. */
	private final ModelValidator validator = new ModelValidator();

	/** Grupos que el modelo agrega solo al proyecto. */
	private final ModelDependencyGroups dependencyGroups;

	/**
	 * Crea el servicio.
	 *
	 * @param persistenceGroup grupo de persistencia, de {@code app.model.persistence-group}
	 * @param validationGroup  grupo de validación, de {@code app.model.validation-group}
	 */
	public ModelService(@Value("${app.model.persistence-group:spring-data}") final String persistenceGroup,
						@Value("${app.model.validation-group:validation}") final String validationGroup) {
		this.dependencyGroups = new ModelDependencyGroups(persistenceGroup, validationGroup);
	}

	/**
	 * Valida un modelo.
	 *
	 * @param model modelo a revisar
	 * @return todos los problemas encontrados
	 */
	public ModelValidation validate(final DomainModel model) {
		return validator.validate(model);
	}

	/**
	 * Calcula los inversos y el mapeo relacional de un modelo.
	 *
	 * @param model modelo, válido o no
	 * @return derivaciones del modelo
	 */
	public ModelDerivations derive(final DomainModel model) {
		return new ModelDerivations(model);
	}

	/**
	 * Grupos del catálogo que el modelo agrega solo al proyecto.
	 *
	 * @return ids de los grupos de persistencia y de validación
	 */
	public ModelDependencyGroups dependencyGroups() {
		return dependencyGroups;
	}
}