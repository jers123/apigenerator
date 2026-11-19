package org.jers.apigenerator.infrastructure.version;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StableVersionsTest {

    @ParameterizedTest
    @ValueSource(strings = {"4.0.0-SNAPSHOT", "1.0-alpha-2", "2.0.0-beta.1", "3.0.0-RC1", "6.0.0-M2",
            "1.0.0.CR2", "13.5.0.jre11-preview", "21-ea", "2025.1.0-M1", "1.0.0-milestone"})
    void unstableVersionsAreExcluded(final String version) {
        assertThat(StableVersions.newestFirst(List.of(version, "1.0.0"), 10)).containsExactly("1.0.0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.1.0.RELEASE", "Hoxton.SR12", "13.2.1.jre11", "33.4.0-jre", "2.4.240"})
    void stableVersionsWithQualifiersAreKept(final String version) {
        assertThat(StableVersions.newestFirst(List.of(version), 10)).containsExactly(version);
    }

    @Test
    void ordersLikeMavenNotAlphabetically() {
        assertThat(StableVersions.newestFirst(List.of("9.9.9", "10.0.0", "Hoxton.SR12", "2025.1.0", "10.0.0"), 10))
                .containsExactly("2025.1.0", "10.0.0", "9.9.9", "Hoxton.SR12");
    }

    @Test
    void respectsTheLimit() {
        assertThat(StableVersions.newestFirst(List.of("1", "2", "3", "4"), 2)).containsExactly("4", "3");
    }
}