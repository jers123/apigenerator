package org.jers.apigenerator.infrastructure.version;

import org.jers.apigenerator.core.version.ArtifactCoordinates;
import org.jers.apigenerator.core.version.ArtifactVersions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MavenCentralVersionProviderTest {

    private static final String H2_METADATA_URL = "https://repo.test/com/h2database/h2/maven-metadata.xml";
    private static final ArtifactCoordinates H2 = new ArtifactCoordinates("com.h2database", "h2");
    private static final String METADATA = """
            <metadata><versioning><versions>
              <version>2.3.232</version><version>2.4.240</version><version>2.5.0-SNAPSHOT</version>
            </versions></versioning></metadata>
            """;

    private MockRestServiceServer server;
    private MavenCentralVersionProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://repo.test/");
        server = MockRestServiceServer.bindTo(builder).build();
        Clock clock = Clock.fixed(Instant.parse("2026-11-07T12:00:00Z"), ZoneOffset.UTC);
        provider = new MavenCentralVersionProvider(builder.build(), clock, Duration.ofHours(6));
    }

    @Test
    void returnsStableVersionsNewestFirst() {
        server.expect(once(), requestTo(H2_METADATA_URL)).andRespond(withSuccess(METADATA, MediaType.APPLICATION_XML));

        ArtifactVersions result = provider.versions(H2);

        assertThat(result.versions()).containsExactly("2.4.240", "2.3.232");
        assertThat(result.latest()).contains("2.4.240");
        server.verify();
    }

    @Test
    void secondCallWithinTtlUsesTheCache() {
        server.expect(once(), requestTo(H2_METADATA_URL)).andRespond(withSuccess(METADATA, MediaType.APPLICATION_XML));

        provider.versions(H2);
        provider.versions(H2);

        server.verify();
    }

    @Test
    void failureIsNotInventedNorCached() {
        server.expect(twice(), requestTo(H2_METADATA_URL)).andRespond(withResourceNotFound());

        ArtifactVersions first = provider.versions(H2);
        provider.versions(H2);

        assertThat(first.available()).isFalse();
        assertThat(first.latest()).isEmpty();
        server.verify();
    }
}