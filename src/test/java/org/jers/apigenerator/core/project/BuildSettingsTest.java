package org.jers.apigenerator.core.project;

import org.jers.apigenerator.core.selection.BuildTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class BuildSettingsTest {

    @Test
    void javaBelowSpringBootMinimumIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BuildSettings(BuildTool.MAVEN, 11, Packaging.JAR, false, 8080))
                .withMessageContaining(String.valueOf(BuildSettings.MIN_JAVA_VERSION));
    }

    @Test
    void minimumJavaVersionIsAccepted() {
        assertThat(new BuildSettings(BuildTool.GRADLE, BuildSettings.MIN_JAVA_VERSION, Packaging.WAR, true, 8080)
                .javaVersion()).isEqualTo(BuildSettings.MIN_JAVA_VERSION);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 65_536})
    void invalidServerPortIsRejected(final int port) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BuildSettings(BuildTool.MAVEN, 25, Packaging.JAR, false, port))
                .withMessageContaining("serverPort");
    }
}