package org.jers.apigenerator.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardinalityTest {

    @Test
    void theInverseIsSeenFromTheTarget() {
        assertThat(Cardinality.MANY_TO_ONE.inverse()).isEqualTo(Cardinality.ONE_TO_MANY);
        assertThat(Cardinality.ONE_TO_MANY.inverse()).isEqualTo(Cardinality.MANY_TO_ONE);
        assertThat(Cardinality.ONE_TO_ONE.inverse()).isEqualTo(Cardinality.ONE_TO_ONE);
        assertThat(Cardinality.MANY_TO_MANY.inverse()).isEqualTo(Cardinality.MANY_TO_MANY);
    }

    @Test
    void onlySingleValuedRelationsCanBeRequired() {
        assertThat(Cardinality.MANY_TO_ONE.allowsRequired()).isTrue();
        assertThat(Cardinality.ONE_TO_ONE.allowsRequired()).isTrue();
        assertThat(Cardinality.ONE_TO_MANY.allowsRequired()).isFalse();
        assertThat(Cardinality.MANY_TO_MANY.allowsRequired()).isFalse();
    }

    @Test
    void oneToManyIsTheOnlyOneOwnedByTheTarget() {
        assertThat(Cardinality.ONE_TO_MANY.declaringSideOwns()).isFalse();
        assertThat(Cardinality.MANY_TO_ONE.declaringSideOwns()).isTrue();
        assertThat(Cardinality.MANY_TO_MANY.declaringSideOwns()).isTrue();
    }
}