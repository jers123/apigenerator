package org.jers.apigenerator.web.model;

import org.jers.apigenerator.core.model.DomainModel;
import org.jers.apigenerator.core.model.ModelDerivations;
import org.jers.apigenerator.core.model.ModelIssue;
import org.jers.apigenerator.core.model.ModelValidation;
import org.jers.apigenerator.core.model.RelationMapping;

import java.util.List;
import java.util.Optional;

/**
 * Respuesta de {@code POST /api/v1/models/validate}: los problemas del
 * modelo y todo lo que el núcleo deriva de él.
 *
 * @param valid        {@code true} si no hay errores; los avisos no cuentan
 * @param issues       problemas, en el orden en que se encontraron
 * @param entities     nombres efectivos e inversos de cada entidad, en orden
 * @param superclasses nombres efectivos de cada superclase, en orden
 * @param relations    mapeo relacional de cada relación cuyo destino existe
 */
public record ModelValidationResponse(
		boolean valid,
		List<ModelIssue> issues,
		List<EntityNamesResponse> entities,
		List<SuperclassNamesResponse> superclasses,
		List<RelationMapping> relations) {

	/**
	 * Arma la respuesta.
	 *
	 * @param model       modelo recibido
	 * @param validation  resultado de validarlo
	 * @param derivations derivaciones del modelo
	 * @return respuesta para la API
	 */
	public static ModelValidationResponse from(final DomainModel model, final ModelValidation validation,
											   final ModelDerivations derivations) {
		List<EntityNamesResponse> entities = model.entities().stream()
				.map(entity -> EntityNamesResponse.from(entity, derivations))
				.toList();
		List<SuperclassNamesResponse> superclasses = model.superclasses().stream()
				.map(SuperclassNamesResponse::from)
				.toList();
		List<RelationMapping> relations = model.entities().stream()
				.flatMap(entity -> entity.relations().stream())
				.map(relation -> derivations.mappingOf(relation.id()))
				.flatMap(Optional::stream)
				.toList();
		return new ModelValidationResponse(!validation.hasErrors(), validation.issues(), entities, superclasses,
				relations);
	}
}