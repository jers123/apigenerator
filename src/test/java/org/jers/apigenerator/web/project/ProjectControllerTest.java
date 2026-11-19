package org.jers.apigenerator.web.project;

import org.jers.apigenerator.application.project.ProjectService;
import org.jers.apigenerator.core.catalog.DependencyCatalogProvider;
import org.jers.apigenerator.core.database.DatabaseEngine;
import org.jers.apigenerator.testsupport.InMemoryDatabaseEngineRepository;
import org.jers.apigenerator.testsupport.InMemoryIdentityProviderRepository;
import org.jers.apigenerator.testsupport.InMemorySecurityCatalogRepository;
import org.jers.apigenerator.testsupport.TestCatalogs;
import org.jers.apigenerator.testsupport.TestIdentityProviders;
import org.jers.apigenerator.testsupport.TestSecurityCatalogs;
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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@Import({ProjectService.class, ProjectControllerTest.Fakes.class})
class ProjectControllerTest {

    @TestConfiguration
    static class Fakes {
        @Bean
        DependencyCatalogProvider catalogProvider() {
            return TestCatalogs::project;
        }

        @Bean
        InMemoryDatabaseEngineRepository databaseEngineRepository() {
            return new InMemoryDatabaseEngineRepository(new DatabaseEngine("postgresql", "PostgreSQL",
                    "postgresql-driver", false, 5432, "postgres", "jdbc:postgresql://{host}:{port}/{database}"));
        }

        @Bean
        InMemorySecurityCatalogRepository securityCatalogRepository() {
            return new InMemorySecurityCatalogRepository(TestSecurityCatalogs.small());
        }

        @Bean
        InMemoryIdentityProviderRepository identityProviderRepository() {
            return new InMemoryIdentityProviderRepository(TestIdentityProviders.small());
        }
    }

    private static final String POSTGRES = """
            {"engineId": "postgresql", "host": "localhost", "port": 5432, "databaseName": "clientes",
             "username": "postgres", "password": "secreto"}""";

    private static final String LAYERED = """
            {"style": "LAYERED", "layout": "SINGLE_MODULE", "patterns": []}""";

    /** Una entidad Cliente con un atributo obligatorio. */
    private static final String CLIENT_MODEL = """
            {"entities": [{"id": "e-1", "name": "Cliente",
              "attributes": [{"id": "a-1", "name": "nombre", "type": {"scalar": "STRING"}, "required": true}]}]}""";

    @Autowired
    private MockMvc mockMvc;

    /** Proyecto Maven con Spring Boot, Spring Web y SpringDoc, con Javadoc y sin pruebas. */
    private static String projectJson(final String artifactId, final String architecture, final String database,
                                      final String versions) {
        return """
                {"metadata": {"groupId": "org.jers", "artifactId": "%s", "name": "API Clientes", "description": "Gestión"},
                 "build": {"buildTool": "MAVEN", "javaVersion": 25, "packaging": "JAR", "reactive": false,
                           "serverPort": 8080},
                 "architecture": %s,
                 "options": {"javadoc": true, "unitTests": false, "configFormat": "YAML"},
                 "dependencyGroupIds": ["spring-boot", "spring-web", "springdoc"],
                 "database": %s,
                 "versions": %s}
                """.formatted(artifactId, architecture, database, versions);
    }

    private ResultActions plan(final String body) throws Exception {
        return mockMvc.perform(post("/api/v1/projects/plan").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** Proyecto sin base de datos con la seguridad indicada. */
    private static String securedJson(final String security) {
        return projectJson("api-clientes", LAYERED, "null", "{}")
                .replace("\"versions\": {}", "\"versions\": {}, \"security\": " + security);
    }

    /** Proyecto con la base indicada y el modelo indicado. */
    private static String modeledJson(final String database, final String model) {
        return projectJson("api-clientes", LAYERED, database, "{}")
                .replace("\"versions\": {}", "\"versions\": {}, \"model\": " + model);
    }

    @Test
    @DisplayName("Devuelve el plan con paquete base, versión de Spring Boot, versiones y URL JDBC")
    void returnsThePlan() throws Exception {
        plan(projectJson("api-clientes", LAYERED, POSTGRES, "{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basePackage").value("org.jers.apiclientes"))
                .andExpect(jsonPath("$.springBootVersion").value("4.1.2"))
                .andExpect(jsonPath("$.dependencies[?(@.artifactId == 'springdoc-openapi-starter-webmvc-ui')].version",
                        contains("3.2.0")))
                .andExpect(jsonPath("$.dependencies[?(@.artifactId == 'spring-boot-starter-webmvc')].managed",
                        contains(true)))
                .andExpect(jsonPath("$.dependencies[*].artifactId", hasItem("postgresql")))
                .andExpect(jsonPath("$.database.jdbcUrl").value("jdbc:postgresql://localhost:5432/clientes"));
    }

    @Test
    @DisplayName("Devuelve las opciones de Javadoc y pruebas tal como se pidieron")
    void returnsGenerationOptions() throws Exception {
        plan(projectJson("api-clientes", LAYERED, "null", "{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options.javadoc").value(true))
                .andExpect(jsonPath("$.options.unitTests").value(false))
                .andExpect(jsonPath("$.options.configFormat").value("YAML"))
                .andExpect(jsonPath("$.persistence").value("IN_MEMORY"))
                .andExpect(jsonPath("$.testPersistence").value("NONE"));
    }

    @Test
    @DisplayName("La contraseña nunca sale en la respuesta")
    void passwordIsNeverReturned() throws Exception {
        plan(projectJson("api-clientes", LAYERED, POSTGRES, "{}"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("secreto"))));
    }

    @Test
    @DisplayName("Aplica las versiones elegidas, incluida la de Spring Boot")
    void appliesChosenVersions() throws Exception {
        plan(projectJson("api-clientes", LAYERED, "null", """
                {"org.springframework.boot:spring-boot-starter-parent": "4.2.0",
                 "org.springdoc:springdoc-openapi-starter-webmvc-ui": "3.3.0"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.springBootVersion").value("4.2.0"))
                .andExpect(jsonPath("$.dependencies[?(@.artifactId == 'springdoc-openapi-starter-webmvc-ui')].version",
                        contains("3.3.0")));
    }

    @Test
    @DisplayName("El plan devuelve la arquitectura con los patrones nativos incluidos")
    void returnsArchitectureWithNativePatterns() throws Exception {
        plan(projectJson("api-clientes", """
                {"style": "HEXAGONAL", "layout": "MULTI_MODULE", "patterns": ["CQRS"]}""", "null", "{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.architecture.style").value("HEXAGONAL"))
                .andExpect(jsonPath("$.architecture.layout").value("MULTI_MODULE"))
                .andExpect(jsonPath("$.architecture.patterns", hasItems("SEPARATED_INTERFACE", "ADAPTER", "CQRS")));
    }

    @Test
    @DisplayName("Un patrón contraindicado responde 400 con su motivo")
    void contraindicatedPatternIsRejected() throws Exception {
        plan(projectJson("api-clientes", """
                {"style": "LAYERED", "layout": "SINGLE_MODULE", "patterns": ["CQRS"]}""", "null", "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("CQRS")));
    }

    @Test
    @DisplayName("Varios módulos con Vertical Slice responde 400")
    void multiModuleVerticalSliceIsRejected() throws Exception {
        plan(projectJson("api-clientes", """
                {"style": "VERTICAL_SLICE", "layout": "MULTI_MODULE"}""", "null", "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Vertical Slice")));
    }

    @Test
    @DisplayName("Fijar la versión de un artefacto de Spring Boot responde 400")
    void versionForSpringBootArtifactIsRejected() throws Exception {
        plan(projectJson("api-clientes", LAYERED, "null", """
                {"org.springframework.boot:spring-boot-starter-webmvc": "4.0.0"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("spring-boot-starter-webmvc")));
    }

    @Test
    @DisplayName("Un motor inexistente responde 400 nombrándolo")
    void unknownEngineIsRejected() throws Exception {
        plan(projectJson("api-clientes", LAYERED, POSTGRES.replace("postgresql", "mariadb"), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("mariadb")));
    }

    @Test
    @DisplayName("Un artifactId inválido responde 400 con la regla del dominio")
    void invalidMetadataIsRejected() throws Exception {
        plan(projectJson("API Clientes", LAYERED, "null", "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", containsString("artifactId")));
    }

    @Test
    @DisplayName("Sin build, arquitectura u opciones la petición se rechaza con 400")
    void missingSectionsAreRejected() throws Exception {
        plan("""
                {"metadata": {"groupId": "org.jers", "artifactId": "api", "name": "API"},
                 "dependencyGroupIds": []}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Con base de desarrollo el plan indica persistencia en base de datos")
    void withDatabaseThePersistenceIsDatabase() throws Exception {
        plan(projectJson("api-clientes", LAYERED, POSTGRES, "{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persistence").value("DATABASE"));
    }

    @Test
    @DisplayName("Una base de pruebas sin base de desarrollo responde 400")
    void testDatabaseWithoutDevelopmentDatabaseIsRejected() throws Exception {
        plan(projectJson("api-clientes", LAYERED, "null", "{}")
                .replace("\"database\": null", "\"database\": null, \"testDatabase\": " + POSTGRES))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("base de desarrollo")));
    }

    @Test
    @DisplayName("Sin formato de configuración la petición se rechaza con 400")
    void missingConfigFormatIsRejected() throws Exception {
        plan(projectJson("api-clientes", LAYERED, "null", "{}").replace(", \"configFormat\": \"YAML\"", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sin seguridad el plan no tiene complementos ni segundo proyecto")
    void withoutSecurityThePlanHasNone() throws Exception {
        plan(projectJson("api-clientes", LAYERED, "null", "{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.security.addOns").isEmpty())
                .andExpect(jsonPath("$.security.managesUsers").value(false));
    }

    @Test
    @DisplayName("Con servidor propio el plan anuncia el segundo proyecto en el puerto 9000")
    void ownServerAnnouncesTheSecondProject() throws Exception {
        plan(securedJson("""
                {"apiAuthenticationId": "oauth2-resource-server", "issuer": {"type": "OWN_SERVER"}}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.security.apiAuthentication").value("OAuth2 Resource Server"))
                .andExpect(jsonPath("$.security.issuerType").value("OWN_SERVER"))
                .andExpect(jsonPath("$.security.issuerUri").value("http://localhost:9000"))
                .andExpect(jsonPath("$.security.authorizationServerArtifactId").value("api-clientes-auth-server"))
                .andExpect(jsonPath("$.security.authorizationServerPort").value(9000))
                .andExpect(jsonPath("$.dependencies[*].artifactId", hasItem("spring-boot-starter-security")));
    }

    @Test
    @DisplayName("Con un proveedor el plan arma la URL del emisor")
    void providerBuildsTheIssuer() throws Exception {
        plan(securedJson("""
                {"apiAuthenticationId": "oauth2-resource-server",
                 "issuer": {"type": "PROVIDER", "providerId": "keycloak",
                            "values": {"baseUrl": "https://kc.example.com", "realm": "ventas"}}}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.security.issuerType").value("PROVIDER"))
                .andExpect(jsonPath("$.security.providerName").value("Keycloak"))
                .andExpect(jsonPath("$.security.issuerUri").value("https://kc.example.com/realms/ventas"));
    }

    @Test
    @DisplayName("Un complemento sin autenticación de la API responde 400")
    void addOnWithoutApiAuthenticationIsRejected() throws Exception {
        plan(securedJson("""
                {"addOnIds": ["cors"]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("autenticación de la API")));
    }

    @Test
    @DisplayName("Un emisor sin tipo responde 400")
    void issuerWithoutTypeIsRejected() throws Exception {
        plan(securedJson("""
                {"apiAuthenticationId": "oauth2-resource-server", "issuer": {"providerId": "keycloak"}}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un modelo con errores responde 400 indicando el paso Modelo")
    void invalidModelIsRejected() throws Exception {
        plan(modeledJson("null", CLIENT_MODEL.replace("\"name\": \"Cliente\"", "\"name\": \"cliente\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("paso Modelo")))
                .andExpect(jsonPath("$.detail", containsString("PascalCase")));
    }

    @Test
    @DisplayName("Un modelo con base de datos agrega solos la persistencia y la validación")
    void modelWithDatabaseAddsPersistenceAndValidation() throws Exception {
        plan(modeledJson(POSTGRES, CLIENT_MODEL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dependencies[*].artifactId",
                        hasItems("spring-boot-starter-data-jpa", "spring-boot-starter-validation")));
    }
}