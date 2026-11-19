package org.jers.apigenerator.core.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ScalarTypeTest {

    @Test
    void onlyTextNumbersAndUuidCanBeIds() {
        assertThat(Arrays.stream(ScalarType.values()).filter(ScalarType::isIdCapable))
                .containsExactlyInAnyOrder(ScalarType.STRING, ScalarType.INTEGER, ScalarType.LONG, ScalarType.UUID);
    }

    @Test
    void eachFamilyAcceptsItsOwnConstraints() {
        assertThat(ScalarType.STRING.acceptsTextRules()).isTrue();
        assertThat(ScalarType.STRING.acceptsRange()).isFalse();

        assertThat(ScalarType.INTEGER.acceptsRange()).isTrue();
        assertThat(ScalarType.INTEGER.acceptsDigits()).isFalse();

        assertThat(ScalarType.BIG_DECIMAL.acceptsRange()).isTrue();
        assertThat(ScalarType.BIG_DECIMAL.acceptsDigits()).isTrue();

        assertThat(ScalarType.LOCAL_DATE.acceptsTemporalRule()).isTrue();
        assertThat(ScalarType.LOCAL_TIME.acceptsTemporalRule()).isFalse();
        assertThat(ScalarType.BOOLEAN.acceptsTextRules() || ScalarType.BOOLEAN.acceptsRange()).isFalse();
    }

    @Test
    void javaLangTypesNeedNoImport() {
        assertThat(ScalarType.STRING.getImportName()).isNull();
        assertThat(ScalarType.LONG.getImportName()).isNull();
        assertThat(ScalarType.BIG_DECIMAL.getImportName()).isEqualTo("java.math.BigDecimal");
        assertThat(ScalarType.UUID.getImportName()).isEqualTo("java.util.UUID");
    }

    @Test
    void exampleValuesAreFixedSoGeneratedTestsAreRepeatable() {
        assertThat(ScalarType.values())
                .allSatisfy(type -> assertThat(type.getExampleValue()).doesNotContain("now()"));
    }
}