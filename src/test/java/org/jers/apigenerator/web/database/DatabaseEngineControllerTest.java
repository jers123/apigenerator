package org.jers.apigenerator.web.database;

import org.jers.apigenerator.application.database.DatabaseEngineService;
import org.jers.apigenerator.core.catalog.DependencyGroup;
import org.jers.apigenerator.testsupport.InMemoryDatabaseEngineRepository;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DatabaseEngineController.class)
@Import({DatabaseEngineService.class, DatabaseEngineControllerTest.Fakes.class})
class DatabaseEngineControllerTest {

    @TestConfiguration
    static class Fakes {
        @Bean
        InMemoryDatabaseEngineRepository databaseEngineRepository() {
            return new InMemoryDatabaseEngineRepository();
        }

        @Bean
        InMemoryDependencyCatalogRepository catalogRepository() {
            return new InMemoryDependencyCatalogRepository(TestCatalogs.small());
        }
    }

    private static final String MARIADB_URL = "jdbc:mariadb://{host}:{port}/{database}";
    private static final String POSTGRES_URL = "jdbc:postgresql://{host}:{port}/{database}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryDatabaseEngineRepository repository;

    @Autowired
    private InMemoryDependencyCatalogRepository catalog;

    @BeforeEach
    void resetState() {
        repository.reset(InMemoryDatabaseEngineRepository.postgresql());
        catalog.reset(TestCatalogs.small());
    }

    /**
     * Cuerpo JSON de un motor en el puerto 3306 con su grupo de driver.
     * El nombre del grupo es "{groupName}" y tiene una sola librería.
     */
    private static String engineJson(final String id, final String label, final String template,
                                     final String groupId, final String groupName, final String category) {
        return """
                {"id": "%s", "label": "%s", "embedded": false, "defaultPort": 3306,
                 "defaultUsername": "root", "jdbcUrlTemplate": "%s",
                 "driverGroup": {"id": "%s", "name": "%s", "category": "%s", "selectedByDefault": false,
                   "dependencies": [{"name": "Driver JDBC", "groupId": "org.example", "artifactId": "%s",
                     "kind": "LIBRARY", "mavenScope": "COMPILE", "gradleScope": "IMPLEMENTATION",
                     "includedByDefault": true, "reactive": false}]}}
                """.formatted(id, label, template, groupId, groupName, category, groupId);
    }

    /** MariaDB con su propio grupo "mariadb-driver". */
    private static String mariadb() {
        return engineJson("mariadb", "MariaDB", MARIADB_URL, "mariadb-driver", "MariaDB Driver", "DATABASE");
    }

    private ResultActions send(final MockHttpServletRequestBuilder request, final String body) throws Exception {
        return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private DependencyGroup group(final String id) {
        return catalog.catalog().findGroup(id).orElseThrow();
    }

    @Test
    @DisplayName("GET lista los motores con su grupo de driver, su tipo y su URL de ejemplo")
    void listsEnginesWithExampleUrl() throws Exception {
        mockMvc.perform(get("/api/v1/databases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("postgresql"))
                .andExpect(jsonPath("$[0].driverGroupId").value("postgresql"))
                .andExpect(jsonPath("$[0].kind").value("RELATIONAL"))
                .andExpect(jsonPath("$[0].exampleUrl").value("jdbc:postgresql://localhost:5432/mi_base"));
    }

    @Test
    @DisplayName("POST crea el motor y su grupo de driver, y responde 201 con Location")
    void createSavesEngineAndGroup() throws Exception {
        send(post("/api/v1/databases"), mariadb())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/databases/mariadb")))
                .andExpect(jsonPath("$.driverGroupId").value("mariadb-driver"))
                .andExpect(jsonPath("$.exampleUrl").value("jdbc:mariadb://localhost:3306/mi_base"));

        assertThat(group("mariadb-driver").name()).isEqualTo("MariaDB Driver");
    }

    @Test
    @DisplayName("POST guarda el tipo y el soporte de secuencias; sin tipo se asume relacional")
    void createKeepsKindAndSequences() throws Exception {
        send(post("/api/v1/databases"), mariadb().replace("\"embedded\": false",
                "\"embedded\": false, \"supportsSequences\": true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("RELATIONAL"))
                .andExpect(jsonPath("$.supportsSequences").value(true));

        assertThat(repository.findById("mariadb").orElseThrow().supportsSequences()).isTrue();
    }

    @Test
    @DisplayName("POST con un motor de documentos responde 400 porque aún no está soportado")
    void documentEngineIsRejected() throws Exception {
        send(post("/api/v1/databases"), mariadb().replace("\"embedded\": false",
                "\"embedded\": false, \"kind\": \"DOCUMENT\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("todavía no están soportados")));

        assertThat(catalog.catalog().findGroup("mariadb-driver")).isEmpty();
    }

    @Test
    @DisplayName("POST con un id de motor existente responde 409 y no crea el grupo")
    void duplicateEngineReturnsConflict() throws Exception {
        send(post("/api/v1/databases"),
                engineJson("postgresql", "Otro", POSTGRES_URL, "otro-driver", "Otro Driver", "DATABASE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Motor de base de datos repetido"));

        assertThat(catalog.catalog().findGroup("otro-driver")).isEmpty();
    }

    @Test
    @DisplayName("POST con un grupo que ya existe responde 409 y no crea el motor")
    void existingGroupReturnsConflict() throws Exception {
        send(post("/api/v1/databases"),
                engineJson("mariadb", "MariaDB", MARIADB_URL, "postgresql", "MariaDB Driver", "DATABASE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Grupo de dependencias repetido"));

        assertThat(repository.findById("mariadb")).isEmpty();
    }

    @Test
    @DisplayName("POST con un grupo que no es de base de datos responde 400")
    void nonDatabaseGroupIsRejected() throws Exception {
        send(post("/api/v1/databases"),
                engineJson("mariadb", "MariaDB", MARIADB_URL, "mariadb-driver", "MariaDB Driver", "OTHER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("DATABASE")));

        assertThat(repository.findById("mariadb")).isEmpty();
    }

    @Test
    @DisplayName("POST con una plantilla inválida responde 400 y no crea el grupo")
    void invalidTemplateReturnsBadRequest() throws Exception {
        send(post("/api/v1/databases"), engineJson("mariadb", "MariaDB", "jdbc:mariadb://{host}:{port}/{db}",
                "mariadb-driver", "MariaDB Driver", "DATABASE"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", containsString("{db}")));

        assertThat(catalog.catalog().findGroup("mariadb-driver")).isEmpty();
    }

    @Test
    @DisplayName("POST sin driverGroup responde 400")
    void missingDriverGroupIsRejected() throws Exception {
        send(post("/api/v1/databases"), """
                {"id": "mariadb", "label": "MariaDB", "embedded": false, "defaultPort": 3306,
                 "defaultUsername": "root", "jdbcUrlTemplate": "%s"}
                """.formatted(MARIADB_URL))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT edita el motor y reemplaza su grupo de driver")
    void updateEditsEngineAndGroup() throws Exception {
        send(put("/api/v1/databases/postgresql"), engineJson("postgresql", "PostgreSQL 18", POSTGRES_URL,
                "postgresql", "PostgreSQL 18 Driver", "DATABASE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("PostgreSQL 18"));

        assertThat(group("postgresql").name()).isEqualTo("PostgreSQL 18 Driver");
    }

    @Test
    @DisplayName("PUT con un grupo nuevo lo crea y lo asigna al motor")
    void updateWithNewGroupCreatesIt() throws Exception {
        send(put("/api/v1/databases/postgresql"), engineJson("postgresql", "PostgreSQL", POSTGRES_URL,
                "postgresql-driver", "PostgreSQL Nuevo", "DATABASE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverGroupId").value("postgresql-driver"));

        assertThat(catalog.catalog().findGroup("postgresql-driver")).isPresent();
    }

    @Test
    @DisplayName("PUT con el grupo de otro motor responde 400 y no cambia nada")
    void groupOfAnotherEngineIsRejected() throws Exception {
        send(post("/api/v1/databases"), mariadb()).andExpect(status().isCreated());

        send(put("/api/v1/databases/postgresql"), engineJson("postgresql", "PostgreSQL", POSTGRES_URL,
                "mariadb-driver", "Robado", "DATABASE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("mariadb")));

        assertThat(group("mariadb-driver").name()).isEqualTo("MariaDB Driver");
    }

    @Test
    @DisplayName("PUT con un grupo existente que no es de base de datos responde 400 y no lo toca")
    void existingNonDatabaseGroupIsRejected() throws Exception {
        send(put("/api/v1/databases/postgresql"), engineJson("postgresql", "PostgreSQL", POSTGRES_URL,
                "lombok", "Lombok Driver", "DATABASE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("lombok")));

        assertThat(group("lombok").name()).isEqualTo("Lombok");
        assertThat(repository.findById("postgresql").orElseThrow().driverGroupId()).isEqualTo("postgresql");
    }

    @Test
    @DisplayName("PUT de un motor inexistente responde 404 y no crea el grupo")
    void updateUnknownEngineReturnsNotFound() throws Exception {
        send(put("/api/v1/databases/mariadb"), mariadb())
                .andExpect(status().isNotFound());

        assertThat(catalog.catalog().findGroup("mariadb-driver")).isEmpty();
    }

    @Test
    @DisplayName("PUT no permite cambiar el id")
    void idInBodyMustMatchUrl() throws Exception {
        send(put("/api/v1/databases/postgresql"), mariadb())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("no se puede cambiar")));
    }
}