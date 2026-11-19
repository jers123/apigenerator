package org.jers.apigenerator.web.version;

import org.jers.apigenerator.application.version.VersionService;
import org.jers.apigenerator.core.version.ArtifactVersions;
import org.jers.apigenerator.core.version.VersionProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VersionController.class)
@Import({VersionService.class, VersionControllerTest.FakeRepository.class})
class VersionControllerTest {

    @TestConfiguration
    static class FakeRepository {
        @Bean
        VersionProvider fakeVersionProvider() {
            return coordinates -> coordinates.artifactId().equals("desconocido")
                    ? ArtifactVersions.unavailable(coordinates)
                    : new ArtifactVersions(coordinates, List.of("2.4.240", "2.3.232"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsLatestAndVersionsForEachArtifact() throws Exception {
        mockMvc.perform(post("/api/v1/versions").contentType(MediaType.APPLICATION_JSON).content("""
                        {"artifacts": [
                          {"groupId": "com.h2database", "artifactId": "h2"},
                          {"groupId": "org.example", "artifactId": "desconocido"}]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifacts[0].latest").value("2.4.240"))
                .andExpect(jsonPath("$.artifacts[0].versions", contains("2.4.240", "2.3.232")))
                .andExpect(jsonPath("$.artifacts[1].available").value(false))
                .andExpect(jsonPath("$.artifacts[1].latest").doesNotExist());
    }

    @Test
    void blankArtifactIdIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/versions").contentType(MediaType.APPLICATION_JSON).content("""
                        {"artifacts": [{"groupId": "com.h2database", "artifactId": " "}]}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void coordinatesWithPathCharactersAreRejected() throws Exception {
        mockMvc.perform(post("/api/v1/versions").contentType(MediaType.APPLICATION_JSON).content("""
                        {"artifacts": [{"groupId": "com.h2database", "artifactId": "../../etc"}]}
                        """))
                .andExpect(status().isBadRequest());
    }
}