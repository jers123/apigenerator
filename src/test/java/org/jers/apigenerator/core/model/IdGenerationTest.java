package org.jers.apigenerator.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdGenerationTest {

    @Test
    void identityAndSequenceNeedIntegralIds() {
        assertThat(IdGeneration.IDENTITY.accepts(ScalarType.LONG)).isTrue();
        assertThat(IdGeneration.IDENTITY.accepts(ScalarType.UUID)).isFalse();
        assertThat(IdGeneration.SEQUENCE.accepts(ScalarType.INTEGER)).isTrue();
        assertThat(IdGeneration.SEQUENCE.accepts(ScalarType.STRING)).isFalse();
    }

    @Test
    void uuidGenerationNeedsUuidOrText() {
        assertThat(IdGeneration.UUID.accepts(ScalarType.UUID)).isTrue();
        assertThat(IdGeneration.UUID.accepts(ScalarType.STRING)).isTrue();
        assertThat(IdGeneration.UUID.accepts(ScalarType.LONG)).isFalse();
    }

    @Test
    void assignedWorksWithEveryIdType() {
        assertThat(IdGeneration.ASSIGNED.getCompatibleTypes())
                .containsExactlyInAnyOrder(ScalarType.STRING, ScalarType.INTEGER, ScalarType.LONG, ScalarType.UUID);
    }

    @Test
    void onlySequenceNeedsSequences() {
        assertThat(IdGeneration.SEQUENCE.needsSequences()).isTrue();
        assertThat(IdGeneration.IDENTITY.needsSequences()).isFalse();
    }

    @Test
    void theDefaultIdIsLongWithIdentity() {
        assertThat(IdSettings.defaults()).isEqualTo(new IdSettings(ScalarType.LONG, IdGeneration.IDENTITY));
    }
}