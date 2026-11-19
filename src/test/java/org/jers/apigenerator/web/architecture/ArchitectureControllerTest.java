package org.jers.apigenerator.web.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArchitectureController.class)
class ArchitectureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Lista los 7 estilos en orden, indicando cuáles admiten varios módulos")
    void listsStyles() throws Exception {
        mockMvc.perform(get("/api/v1/architectures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.styles[*].id", contains("LAYERED", "PACKAGE_BY_FEATURE", "HEXAGONAL",
                        "ONION", "CLEAN", "VERTICAL_SLICE", "MODULITH")))
                .andExpect(jsonPath("$.styles[?(@.id == 'VERTICAL_SLICE')].multiModule", contains(false)))
                .andExpect(jsonPath("$.styles[?(@.id == 'HEXAGONAL')].multiModule", contains(true)))
                .andExpect(jsonPath("$.layouts[*].id", contains("SINGLE_MODULE", "MULTI_MODULE")));
    }

    @Test
    @DisplayName("Trae todos los patrones, incluso los que no se pueden elegir, con compatibilidad y motivo")
    void patternsCarryCompatibilityAndReason() throws Exception {
        mockMvc.perform(get("/api/v1/architectures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patterns[?(@.id == 'CQRS')].compatibility.LAYERED", contains("CONTRAINDICATED")))
                .andExpect(jsonPath("$.patterns[?(@.id == 'CQRS')].compatibility.VERTICAL_SLICE", contains("NATIVE")))
                .andExpect(jsonPath("$.patterns[?(@.id == 'CQRS')].reason", contains(containsString("único modelo"))))
                .andExpect(jsonPath("$.patterns[?(@.id == 'API_GATEWAY')].compatibility.HEXAGONAL",
                        contains("NOT_APPLICABLE")))
                .andExpect(jsonPath("$.patterns[?(@.id == 'STRATEGY')].familyLabel",
                        contains("De comportamiento (GoF)")));
    }
}