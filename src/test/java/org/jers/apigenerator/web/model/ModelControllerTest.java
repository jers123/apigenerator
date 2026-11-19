package org.jers.apigenerator.web.model;

import org.jers.apigenerator.application.model.ModelService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModelController.class)
@Import(ModelService.class)
class ModelControllerTest {

	/** Cliente (nombre) y Pedido (fecha) con Pedido N‑1 Cliente obligatoria, inverso "pedidos". */
	private static final String CLIENTS_AND_ORDERS = """
            {"idSettings": {"type": "LONG", "generation": "IDENTITY"},
             "entities": [
               {"id": "e-cliente", "name": "Cliente",
                "attributes": [{"id": "a-nombre", "name": "nombre", "type": {"scalar": "STRING"}, "required": true}]},
               {"id": "e-pedido", "name": "Pedido",
                "attributes": [{"id": "a-fecha", "name": "fecha", "type": {"scalar": "LOCAL_DATE"}, "required": true}],
                "relations": [{"id": "r-cliente", "name": "cliente", "targetEntityId": "e-cliente",
                               "cardinality": "MANY_TO_ONE", "required": true, "inverseName": "pedidos"}]}]}
            """;

	/** Superclase Base (activo, columna es_activo) con el id en la raíz y Cliente que hereda de ella. */
	private static final String WITH_SUPERCLASS = """
            {"idSettings": {"type": "LONG", "generation": "IDENTITY", "placement": "ROOT_SUPERCLASS"},
             "audit": {"createdAt": true, "updatedAt": false},
             "superclasses": [
               {"id": "s-base", "name": "Base",
                "attributes": [{"id": "a-activo", "name": "activo", "columnName": "es_activo",
                                "type": {"scalar": "BOOLEAN"}}]}],
             "entities": [
               {"id": "e-cliente", "name": "Cliente", "superclassId": "s-base",
                "attributes": [{"id": "a-nombre", "name": "nombre", "type": {"scalar": "STRING"}}]}]}
            """;

	@Autowired
	private MockMvc mockMvc;

	private ResultActions validate(final String body) throws Exception {
		return mockMvc.perform(post("/api/v1/models/validate").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	@DisplayName("Un modelo válido responde sin problemas, con nombres derivados, inversos y mapeos")
	void validModelReturnsDerivedNames() throws Exception {
		validate(CLIENTS_AND_ORDERS)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valid").value(true))
				.andExpect(jsonPath("$.issues").isEmpty())
				.andExpect(jsonPath("$.entities[0].tableName").value("cliente"))
				.andExpect(jsonPath("$.entities[0].resourcePath").value("/clientes"))
				.andExpect(jsonPath("$.entities[0].inverses[0].name").value("pedidos"))
				.andExpect(jsonPath("$.entities[0].inverses[0].cardinality").value("ONE_TO_MANY"))
				.andExpect(jsonPath("$.entities[1].columns['a-fecha']").value("fecha"))
				.andExpect(jsonPath("$.superclasses").isEmpty())
				.andExpect(jsonPath("$.relations[0].ownerEntityId").value("e-pedido"))
				.andExpect(jsonPath("$.relations[0].joinColumn").value("cliente_id"));
	}

	@Test
	@DisplayName("Devuelve las columnas de los atributos de cada superclase")
	void returnsSuperclassColumns() throws Exception {
		validate(WITH_SUPERCLASS)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valid").value(true))
				.andExpect(jsonPath("$.superclasses[0].superclassId").value("s-base"))
				.andExpect(jsonPath("$.superclasses[0].columns['a-activo']").value("es_activo"));
	}

	@Test
	@DisplayName("Con el id en la raíz, una entidad sin superclase es un error")
	void rootPlacementNeedsInheritance() throws Exception {
		validate(WITH_SUPERCLASS.replace(", \"superclassId\": \"s-base\"", ""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valid").value(false))
				.andExpect(jsonPath("$.issues[?(@.elementId == 'e-cliente')].message",
						hasItem(containsString("Debe heredar"))));
	}

	@Test
	@DisplayName("Un modelo con errores responde 200, valid=false y cada problema con su elemento")
	void invalidModelStillReturnsOk() throws Exception {
		validate(CLIENTS_AND_ORDERS.replace("\"name\": \"Cliente\"", "\"name\": \"cliente\""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valid").value(false))
				.andExpect(jsonPath("$.issues[0].severity").value("ERROR"))
				.andExpect(jsonPath("$.issues[0].elementId").value("e-cliente"))
				.andExpect(jsonPath("$.issues[0].location").value("cliente"))
				.andExpect(jsonPath("$.issues[0].message", containsString("PascalCase")));
	}

	@Test
	@DisplayName("Un aviso no invalida el modelo")
	void warningsKeepTheModelValid() throws Exception {
		validate("""
                {"entities": [{"id": "e-1", "name": "Etiqueta"}]}
                """)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valid").value(true))
				.andExpect(jsonPath("$.issues[0].severity").value("WARNING"));
	}

	@Test
	@DisplayName("Un cuerpo vacío es un modelo vacío y válido")
	void emptyBodyIsAnEmptyModel() throws Exception {
		validate("{}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valid").value(true))
				.andExpect(jsonPath("$.entities").isEmpty());
	}

	@Test
	@DisplayName("Un elemento sin id interno responde 400 con el mensaje del dominio")
	void missingInternalIdIsBadRequest() throws Exception {
		validate("""
                {"entities": [{"name": "Cliente"}]}
                """)
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Datos inválidos"))
				.andExpect(jsonPath("$.detail", containsString("id interno")));
	}

	@Test
	@DisplayName("Informa los grupos que el modelo agrega solos, con los valores de fábrica")
	void returnsTheModelDependencyGroups() throws Exception {
		mockMvc.perform(get("/api/v1/models/dependency-groups"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.persistenceGroupId").value("spring-data"))
				.andExpect(jsonPath("$.validationGroupId").value("validation"));
	}
}