package org.jers.apigenerator.web.catalog;

import org.jers.apigenerator.application.catalog.DependencyGroupService;
import org.jers.apigenerator.core.catalog.DependencyGroup;
import org.jers.apigenerator.testsupport.InMemoryDependencyCatalogRepository;
import org.jers.apigenerator.testsupport.TestCatalogs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DependencyGroupController.class)
@Import({DependencyGroupService.class, DependencyGroupControllerTest.Fakes.class})
class DependencyGroupControllerTest {

    @TestConfiguration
    static class Fakes {
        @Bean
        InMemoryDependencyCatalogRepository catalogRepository() {
            return new InMemoryDependencyCatalogRepository(TestCatalogs.small());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryDependencyCatalogRepository repository;

    @BeforeEach
    void resetCatalog() {
        repository.reset(TestCatalogs.small());
    }

    /** Cuerpo JSON de un grupo con una sola librería Kafka. */
    private static String groupJson(final String id, final String name, final String category) {
        return """
                {"id": "%s", "name": "%s", "category": "%s", "selectedByDefault": false,
                 "dependencies": [{"name": "Kafka", "groupId": "org.springframework.kafka",
                   "artifactId": "spring-kafka", "version": null, "kind": "LIBRARY",
                   "mavenScope": "COMPILE", "gradleScope": "IMPLEMENTATION",
                   "includedByDefault": true, "reactive": false}]}
                """.formatted(id, name, category);
    }

    private ResultActions send(final MockHttpServletRequestBuilder request, final String body) throws Exception {
        return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String nameOf(final String id) {
        return repository.catalog().findGroup(id).map(DependencyGroup::name).orElseThrow();
    }

    @Test
    @DisplayName("GET devuelve la definición con todas las dependencias, sin dividir por modo")
    void getReturnsTheRawDefinition() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/groups/spring-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("spring-web"))
                .andExpect(jsonPath("$.category").value("SPRING"))
                .andExpect(jsonPath("$.dependencies[*].name", contains("WebMVC", "WebFlux")))
                .andExpect(jsonPath("$.dependencies[1].reactive").value(true));
    }

    @Test
    @DisplayName("GET de un grupo inexistente responde 404")
    void getUnknownGroupReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/groups/kafka"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Grupo de dependencias no encontrado"));
    }

    @Test
    @DisplayName("POST crea el grupo al final del catálogo y responde 201 con Location")
    void createAddsTheGroup() throws Exception {
        send(post("/api/v1/catalog/groups"), groupJson("kafka", "Spring for Kafka", "SPRING"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/catalog/groups/kafka")))
                .andExpect(jsonPath("$.dependencies[0].artifactId").value("spring-kafka"));

        assertThat(repository.catalog().groups()).last().extracting(DependencyGroup::id).isEqualTo("kafka");
    }

    @Test
    @DisplayName("POST con un id existente responde 409")
    void duplicateIdReturnsConflict() throws Exception {
        send(post("/api/v1/catalog/groups"), groupJson("lombok", "Otro Lombok", "OTHER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Grupo de dependencias repetido"));
    }

    @Test
    @DisplayName("POST con un nombre existente responde 400 nombrándolo")
    void duplicateNameReturnsBadRequest() throws Exception {
        send(post("/api/v1/catalog/groups"), groupJson("lombok-2", "Lombok", "OTHER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Lombok")));
    }

    @Test
    @DisplayName("POST de un grupo de base de datos responde 400 y no lo guarda")
    void databaseGroupIsRejected() throws Exception {
        send(post("/api/v1/catalog/groups"), groupJson("mariadb-driver", "MariaDB Driver", "DATABASE"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", containsString("base de datos")));

        assertThat(repository.catalog().findGroup("mariadb-driver")).isEmpty();
    }

    @Test
    @DisplayName("POST con una librería sin scopes responde 400 con la regla del dominio")
    void libraryWithoutScopesIsRejected() throws Exception {
        send(post("/api/v1/catalog/groups"), groupJson("kafka", "Kafka", "SPRING")
                .replace("\"mavenScope\": \"COMPILE\",", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("mavenScope")));
    }

    @Test
    @DisplayName("POST sin categoría o sin dependencias responde 400")
    void missingRequiredFieldsAreRejected() throws Exception {
        send(post("/api/v1/catalog/groups"), """
                {"id": "kafka", "name": "Kafka", "dependencies": []}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT renombra un grupo conservando su id y su posición")
    void updateRenamesTheGroup() throws Exception {
        send(put("/api/v1/catalog/groups/lombok"), groupJson("lombok", "Project Lombok", "OTHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Project Lombok"));

        assertThat(nameOf("lombok")).isEqualTo("Project Lombok");
        assertThat(repository.catalog().groups().get(3).id()).isEqualTo("lombok");
    }

    @Test
    @DisplayName("PUT de un grupo de base de datos existente responde 400 aunque cambie la categoría")
    void existingDatabaseGroupCannotBeEditedHere() throws Exception {
        send(put("/api/v1/catalog/groups/postgresql"), groupJson("postgresql", "PostgreSQL", "OTHER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("base de datos")));

        assertThat(nameOf("postgresql")).isEqualTo("PostgreSQL Driver");
    }

    @Test
    @DisplayName("PUT que convierte un grupo en uno de base de datos responde 400")
    void groupCannotBecomeADatabaseGroup() throws Exception {
        send(put("/api/v1/catalog/groups/lombok"), groupJson("lombok", "Lombok", "DATABASE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT de un grupo inexistente responde 404")
    void updateUnknownGroupReturnsNotFound() throws Exception {
        send(put("/api/v1/catalog/groups/kafka"), groupJson("kafka", "Kafka", "SPRING"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT no permite cambiar el id")
    void idInBodyMustMatchUrl() throws Exception {
        send(put("/api/v1/catalog/groups/lombok"), groupJson("lombok-2", "Lombok", "OTHER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("no se puede cambiar")));
    }

    @Test
    @DisplayName("PUT con el nombre de otro grupo responde 400 y no cambia nada")
    void renameToAnExistingNameIsRejected() throws Exception {
        send(put("/api/v1/catalog/groups/lombok"), groupJson("lombok", "Spring Web", "OTHER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Spring Web")));

        assertThat(nameOf("lombok")).isEqualTo("Lombok");
    }

    @Test
    @DisplayName("La marca 'necesita base de datos' se guarda y se devuelve")
    void requiresDatabaseIsStoredAndReturned() throws Exception {
        send(post("/api/v1/catalog/groups"), groupJson("jpa", "JPA", "SPRING")
                .replace("\"selectedByDefault\": false,", "\"selectedByDefault\": false, \"requiresDatabase\": true,"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requiresDatabase").value(true));

        assertThat(repository.catalog().findGroup("jpa").orElseThrow().requiresDatabase()).isTrue();
    }
}