package org.jers.apigenerator.web.security;

import org.jers.apigenerator.application.security.SecurityMechanismService;
import org.jers.apigenerator.core.security.SecurityMechanism;
import org.jers.apigenerator.testsupport.InMemorySecurityCatalogRepository;
import org.jers.apigenerator.testsupport.TestSecurityCatalogs;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityMechanismController.class)
@Import({SecurityMechanismService.class, SecurityMechanismControllerTest.Fakes.class})
class SecurityMechanismControllerTest {

    @TestConfiguration
    static class Fakes {
        @Bean
        InMemorySecurityCatalogRepository securityCatalogRepository() {
            return new InMemorySecurityCatalogRepository(TestSecurityCatalogs.small());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemorySecurityCatalogRepository repository;

    @BeforeEach
    void resetCatalog() {
        repository.reset(TestSecurityCatalogs.small());
    }

    /** Cuerpo JSON de un método con el starter de seguridad. */
    private static String mechanismJson(final String id, final String name, final String role,
                                        final boolean requiresIssuer, final String conflicts) {
        return """
                {"id": "%s", "name": "%s", "description": "Prueba", "role": "%s",
                 "reactiveSupported": true, "requiresIssuer": %s, "managesUsers": false, "conflictsWith": %s,
                 "dependencies": [{"name": "Spring Security", "groupId": "org.springframework.boot",
                   "artifactId": "spring-boot-starter-security", "kind": "LIBRARY",
                   "mavenScope": "COMPILE", "gradleScope": "IMPLEMENTATION",
                   "includedByDefault": true, "reactive": false}]}
                """.formatted(id, name, role, requiresIssuer, conflicts);
    }

    private ResultActions send(final MockHttpServletRequestBuilder request, final String body) throws Exception {
        return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String nameOf(final String id) {
        return repository.catalog().findMechanism(id).map(SecurityMechanism::name).orElseThrow();
    }

    @Test
    @DisplayName("GET lista los métodos en orden con su tipo")
    void listsMechanisms() throws Exception {
        mockMvc.perform(get("/api/v1/security/mechanisms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains("http-basic", "oauth2-resource-server", "form-login", "cors",
                        "authorization-server")))
                .andExpect(jsonPath("$[2].role").value("BROWSER_LOGIN"))
                .andExpect(jsonPath("$[2].conflictsWith", contains("http-basic")));
    }

    @Test
    @DisplayName("GET de un método inexistente responde 404")
    void unknownMechanismReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/security/mechanisms/kerberos"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Método de seguridad no encontrado"));
    }

    @Test
    @DisplayName("POST crea el método al final y responde 201 con Location")
    void createAddsTheMechanism() throws Exception {
        send(post("/api/v1/security/mechanisms"), mechanismJson("api-key", "API Key", "API_AUTHENTICATION", false, "[]"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/security/mechanisms/api-key")));

        assertThat(repository.catalog().mechanisms()).last().extracting(SecurityMechanism::id).isEqualTo("api-key");
    }

    @Test
    @DisplayName("POST con un id existente responde 409")
    void duplicateIdReturnsConflict() throws Exception {
        send(post("/api/v1/security/mechanisms"), mechanismJson("cors", "Otro CORS", "ADD_ON", false, "[]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Método de seguridad repetido"));
    }

    @Test
    @DisplayName("POST de un segundo servidor de autorización responde 400")
    void secondAuthorizationServerIsRejected() throws Exception {
        send(post("/api/v1/security/mechanisms"),
                mechanismJson("other-server", "Otro servidor", "AUTHORIZATION_SERVER", false, "[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Servidor de autorización")));
    }

    @Test
    @DisplayName("POST con un choque contra un método inexistente responde 400 nombrándolo")
    void conflictWithUnknownMechanismIsRejected() throws Exception {
        send(post("/api/v1/security/mechanisms"),
                mechanismJson("api-key", "API Key", "API_AUTHENTICATION", false, "[\"kerberos\"]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("kerberos")));
    }

    @Test
    @DisplayName("POST de un complemento que pide emisor responde 400")
    void addOnRequiringIssuerIsRejected() throws Exception {
        send(post("/api/v1/security/mechanisms"), mechanismJson("csrf", "CSRF", "ADD_ON", true, "[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("emisor")));
    }

    @Test
    @DisplayName("PUT renombra un método conservando su posición")
    void updateRenamesTheMechanism() throws Exception {
        send(put("/api/v1/security/mechanisms/cors"), mechanismJson("cors", "CORS abierto", "ADD_ON", false, "[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("CORS abierto"));

        assertThat(nameOf("cors")).isEqualTo("CORS abierto");
        assertThat(repository.catalog().mechanisms().get(3).id()).isEqualTo("cors");
    }

    @Test
    @DisplayName("PUT no permite cambiar el id")
    void idInBodyMustMatchUrl() throws Exception {
        send(put("/api/v1/security/mechanisms/cors"), mechanismJson("cors-2", "CORS", "ADD_ON", false, "[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("no se puede cambiar")));
    }

    @Test
    @DisplayName("PUT de un método inexistente responde 404")
    void updateUnknownMechanismReturnsNotFound() throws Exception {
        send(put("/api/v1/security/mechanisms/kerberos"), mechanismJson("kerberos", "Kerberos", "ADD_ON", false, "[]"))
                .andExpect(status().isNotFound());
    }
}