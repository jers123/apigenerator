package org.jers.apigenerator.web.model;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jers.apigenerator.application.model.ModelService;
import org.jers.apigenerator.core.model.DomainModel;
import org.jers.apigenerator.core.project.ModelDependencyGroups;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST del modelo de dominio.
 */
@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
@Tag(name = "Modelo", description = "Validación y nombres derivados del modelo de dominio")
public class ModelController {

	/** Casos de uso del modelo. */
	private final ModelService service;

	/**
	 * Valida el modelo y devuelve sus problemas y nombres derivados. Siempre
	 * responde 200: tener errores es el resultado, no un fallo de la petición.
	 *
	 * @param model modelo tal como está en la pantalla
	 * @return problemas, nombres efectivos, inversos y mapeo de relaciones
	 */
	@PostMapping("/validate")
	@Operation(summary = "Valida el modelo y devuelve sus problemas y nombres derivados")
	public ModelValidationResponse validate(@RequestBody final DomainModel model) {
		return ModelValidationResponse.from(model, service.validate(model), service.derive(model));
	}

	/**
	 * Grupos del catálogo que el modelo agrega solos al proyecto, para que la
	 * pantalla de dependencias los muestre incluidos y bloqueados.
	 *
	 * @return ids de los grupos de persistencia y de validación
	 */
	@GetMapping("/dependency-groups")
	@Operation(summary = "Grupos del catálogo que el modelo agrega solos al proyecto")
	public ModelDependencyGroups dependencyGroups() {
		return service.dependencyGroups();
	}
}