package org.jers.apigenerator.core.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AttributeConstraintsTest {

    @Test
    void noneHasNoRule() {
        assertThat(AttributeConstraints.none().hasAny()).isFalse();
    }

    @Test
    void anySingleRuleCounts() {
        assertThat(new AttributeConstraints(null, null, BigDecimal.ONE, null, null, null, null, false, null).hasAny())
                .isTrue();
        assertThat(new AttributeConstraints(null, null, null, null, null, null, null, true, null).hasAny())
                .isTrue();
        assertThat(new AttributeConstraints(null, null, null, null, null, null, null, false, TemporalRule.PAST)
                .hasAny()).isTrue();
    }

    @Test
    void aBlankPatternMeansNoPattern() {
        AttributeConstraints constraints = new AttributeConstraints(null, null, null, null, null, null, " ", false,
                null);

        assertThat(constraints.pattern()).isNull();
        assertThat(constraints.hasAny()).isFalse();
    }
}