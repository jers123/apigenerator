package org.jers.apigenerator.core.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DependencyTest {

    @Test
    void validLibraryExposesItsCoordinates() {
        Dependency lombok = CatalogFixtures.library("Lombok");

        assertThat(lombok.coordinates()).isEqualTo("org.example:lombok");
        assertThat(lombok.hasExplicitVersion()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void blankArtifactIdIsRejected(final String artifactId) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Dependency("X", "org.example", artifactId, null,
                        DependencyKind.LIBRARY, MavenScope.COMPILE, GradleScope.IMPLEMENTATION,
                        true, false))
                .withMessageContaining("artifactId");
    }

    @Test
    void libraryWithoutScopesIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Dependency("H2", "com.h2database", "h2", null,
                        DependencyKind.LIBRARY, null, null, true, false))
                .withMessageContaining("mavenScope");
    }

    @Test
    void pluginDoesNotNeedScopes() {
        Dependency plugin = new Dependency("Compiler", "org.apache.maven.plugins",
                "maven-compiler-plugin", "3.15.0", DependencyKind.PLUGIN, null, null, true, false);

        assertThat(plugin.hasExplicitVersion()).isTrue();
    }

    @Test
    void blankVersionIsRejectedBecauseNullAlreadyMeansManaged() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Dependency("X", "org.example", "x", " ",
                        DependencyKind.LIBRARY, MavenScope.COMPILE, GradleScope.IMPLEMENTATION,
                        true, false))
                .withMessageContaining("version");
    }

    @Test
    void reactiveDependencyCannotBeIncludedByDefault() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Dependency("WebFlux", "org.example", "webflux", null,
                        DependencyKind.LIBRARY, MavenScope.COMPILE, GradleScope.IMPLEMENTATION,
                        true, true))
                .withMessageContaining("WebFlux");
    }

    @Test
    void springBootStartersFollowTheParentButTheParentDoesNot() {
        Dependency starter = new Dependency("Web", "org.springframework.boot", "spring-boot-starter-webmvc", null,
                DependencyKind.LIBRARY, MavenScope.COMPILE, GradleScope.IMPLEMENTATION, true, false);
        Dependency parent = new Dependency("Parent", "org.springframework.boot", "spring-boot-starter-parent", "4.1.2",
                DependencyKind.PARENT, null, null, true, false);

        assertThat(starter.followsSpringBootVersion()).isTrue();
        assertThat(parent.followsSpringBootVersion()).isFalse();
        assertThat(CatalogFixtures.library("Lombok").followsSpringBootVersion()).isFalse();
    }
}