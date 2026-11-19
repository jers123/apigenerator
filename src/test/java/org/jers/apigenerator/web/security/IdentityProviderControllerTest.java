package org.jers.apigenerator.web.security;

import org.jers.apigenerator.application.security.IdentityProviderService;
import org.jers.apigenerator.testsupport.InMemoryIdentityProviderRepository;
import org.jers.apigenerator.testsupport.TestIdentityProviders;
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

@WebMvcTest(IdentityProviderController.class)
@Import({IdentityProviderService.class, IdentityProviderControllerTest.Fakes.class})
class IdentityProviderControllerTest {

    @TestConfiguration
    static class Fakes {
        @Bean
        InMemoryIdentityProviderRepository identityProviderRepository() {
            return new InMemoryIdentityProviderRepository(TestIdentityProviders.small());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryIdentityProviderRepository repository;

    @BeforeEach
    void resetCatalog() {
        repository.reset(TestIdentityProviders.small());
    }

    /** Cuerpo JSON de un proveedor en la nube con un parámetro {@code domain}. */
    private static String providerJson(final String id, final String name, final String template,
                                       final String parameters) {
        return """
                {"id": "%s", "name": "%s", "description": "Prueba", "hosting": "CLOUD",
                 "issuerUriTemplate": "%s", "parameters": %s,
                 "supportsResourceServer": true, "supportsLogin": true,
                 "documentationUrl": "https://example.com/docs"}
                """.formatted(id, name, template, parameters);
    }

    private static final String DOMAIN = """
            [{"key": "domain", "label": "Dominio", "example": "t.us.auth0.com"}]""";

    private ResultActions send(final MockHttpServletRequestBuilder request, final String body) throws Exception {
        return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    @DisplayName("GET lista los proveedores con sus parámetros")
    void listsProviders() throws Exception {
        mockMvc.perform(get("/api/v1/security/identity-providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains("keycloak", "github")))
                .andExpect(jsonPath("$[0].parameters[*].key", contains("baseUrl", "realm")))
                .andExpect(jsonPath("$[1].supportsResourceServer").value(false));
    }

    @Test
    @DisplayName("GET de un proveedor inexistente responde 404")
    void unknownProviderReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/security/identity-providers/ping"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Proveedor de identidad no encontrado"));
    }

    @Test
    @DisplayName("POST crea el proveedor al final y responde 201 con Location")
    void createAddsTheProvider() throws Exception {
        send(post("/api/v1/security/identity-providers"), providerJson("auth0", "Auth0", "https://{domain}/", DOMAIN))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/security/identity-providers/auth0")));

        assertThat(repository.catalog().findProvider("auth0")).isPresent();
    }

    @Test
    @DisplayName("POST con un id existente responde 409")
    void duplicateIdReturnsConflict() throws Exception {
        send(post("/api/v1/security/identity-providers"),
                providerJson("keycloak", "Otro Keycloak", "https://{domain}/", DOMAIN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Proveedor de identidad repetido"));
    }

    @Test
    @DisplayName("POST con un marcador sin parámetro responde 400 nombrándolo")
    void placeholderWithoutParameterIsRejected() throws Exception {
        send(post("/api/v1/security/identity-providers"),
                providerJson("entra-id", "Entra ID", "https://login.microsoftonline.com/{tenantId}/v2.0", "[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("tenantId")));
    }

    @Test
    @DisplayName("PUT renombra un proveedor")
    void updateRenamesTheProvider() throws Exception {
        send(put("/api/v1/security/identity-providers/keycloak"), providerJson("keycloak", "Keycloak propio",
                "{baseUrl}/realms/{realm}", """
                        [{"key": "baseUrl", "label": "URL"}, {"key": "realm", "label": "Realm"}]"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Keycloak propio"));
    }

    @Test
    @DisplayName("PUT no permite cambiar el id")
    void idInBodyMustMatchUrl() throws Exception {
        send(put("/api/v1/security/identity-providers/keycloak"),
                providerJson("auth0", "Auth0", "https://{domain}/", DOMAIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("no se puede cambiar")));
    }
}