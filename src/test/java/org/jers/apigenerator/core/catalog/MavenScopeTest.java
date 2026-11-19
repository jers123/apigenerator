package org.jers.apigenerator.core.catalog;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class MavenScopeTest {
    @ParameterizedTest(name = "{0}")
    @EnumSource(MavenScope.class)
    void valueIsTheLowercaseNameAsMavenExpects(final MavenScope scope) {
        assertThat(scope.getValue()).isEqualTo(scope.name().toLowerCase(Locale.ROOT));
    }
}