package org.jers.apigenerator.core.catalog;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class GradleScopeTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(GradleScope.class)
    void valueIsTheCamelCaseNameAsGradleExpects(final GradleScope scope) {
        assertThat(scope.getValue()).isEqualTo(toCamelCase(scope.name()));
    }

    /** Convierte TEST_RUNTIME_ONLY en testRuntimeOnly. */
    private static String toCamelCase(final String constant) {
        String[] parts = constant.toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            result.append(Character.toUpperCase(parts[i].charAt(0)))
                    .append(parts[i].substring(1));
        }
        return result.toString();
    }
}