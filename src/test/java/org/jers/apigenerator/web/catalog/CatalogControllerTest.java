package org.jers.apigenerator.web.catalog;

import org.jers.apigenerator.application.catalog.CatalogService;
import org.jers.apigenerator.core.catalog.DependencyCatalogProvider;
import org.jers.apigenerator.testsupport.TestCatalogs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogController.class)
@Import({CatalogService.class, CatalogControllerTest.TestCatalogConfig.class})
class CatalogControllerTest {

    @TestConfiguration
    static class TestCatalogConfig {
        @Bean
        DependencyCatalogProvider testCatalogProvider() {
            return TestCatalogs::small;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /catalog devuelve los grupos en orden con su id y su categoría legible")
    void catalogListsGroups() throws Exception {
        mockMvc.perform(get("/api/v1/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[0].id").value("maven"))
                .andExpect(jsonPath("$.groups[0].name").value("Maven"))
                .andExpect(jsonPath("$.groups[2].id").value("spring-web"))
                .andExpect(jsonPath("$.groups[2].category").value("SPRING"))
                .andExpect(jsonPath("$.groups[2].categoryLabel").value("Spring"))
                .andExpect(jsonPath("$.groups[2].standardDependencies[*].name", contains("WebMVC")))
                .andExpect(jsonPath("$.groups[2].reactiveDependencies[*].name", contains("WebFlux")));
    }

    @Test
    @DisplayName("POST /resolve aplica las reglas de selección")
    void resolveAppliesRules() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupIds": ["spring-web"], "buildTool": "MAVEN", "reactive": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupIds", contains("maven", "spring-web")))
                .andExpect(jsonPath("$.dependencies[*].name", contains("Maven Plugin", "WebFlux")));
    }

    @Test
    @DisplayName("Un grupo inexistente responde 400 con ProblemDetail")
    void unknownGroupReturnsProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupIds": ["kafka"], "buildTool": "MAVEN", "reactive": false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Grupo de dependencias inexistente"))
                .andExpect(jsonPath("$.groupIds", contains("kafka")));
    }

    @Test
    @DisplayName("Sin buildTool la petición se rechaza con 400")
    void missingBuildToolIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupIds": [], "reactive": false}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Una herramienta de build desconocida se rechaza con 400")
    void unknownBuildToolIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupIds": [], "buildTool": "ANT", "reactive": false}
                                """))
                .andExpect(status().isBadRequest());
    }
}