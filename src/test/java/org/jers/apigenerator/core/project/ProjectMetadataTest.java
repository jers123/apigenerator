package org.jers.apigenerator.core.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ProjectMetadataTest {

    @Test
    void basePackageJoinsGroupAndArtifactWithoutHyphens() {
        assertThat(ProjectFixtures.metadata().basePackage()).isEqualTo("org.jers.apiclientes");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Org.Jers", "org..jers", "1org.jers", "org.jers."})
    void invalidGroupIdIsRejectedWithItsValue(final String groupId) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ProjectMetadata(groupId, "api", "API", null))
                .withMessageContaining("groupId")
                .withMessageContaining(groupId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Api", "api--clientes", "-api", "api-", "1api", "api_clientes"})
    void invalidArtifactIdIsRejected(final String artifactId) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ProjectMetadata("org.jers", artifactId, "API", null))
                .withMessageContaining("artifactId");
    }

    @Test
    void missingDescriptionBecomesEmpty() {
        assertThat(new ProjectMetadata("org.jers", "api", "API", null).description()).isEmpty();
    }
}