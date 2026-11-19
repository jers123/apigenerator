package org.jers.apigenerator.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class RelationTest {

    private static Relation relation(final String inverseName) {
        return new Relation("r-1", "cliente", "e-cliente", Cardinality.MANY_TO_ONE, true, inverseName, null, null);
    }

    @Test
    void itIsBidirectionalOnlyWithAnInverseName() {
        assertThat(relation(" ").bidirectional()).isFalse();
        assertThat(relation(null).bidirectional()).isFalse();
        assertThat(relation("pedidos").bidirectional()).isTrue();
    }

    @Test
    void blankJoinNamesMeanDerived() {
        Relation relation = new Relation("r-1", " cliente ", "e-cliente", Cardinality.MANY_TO_MANY, false, null,
                " ", "");

        assertThat(relation.name()).isEqualTo("cliente");
        assertThat(relation.joinColumn()).isNull();
        assertThat(relation.joinTable()).isNull();
    }

    @Test
    void itNeedsAnInternalIdAndACardinality() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Relation("", "cliente", "e-cliente", Cardinality.MANY_TO_ONE, true, null,
                        null, null))
                .withMessageContaining("cliente");
        assertThatNullPointerException()
                .isThrownBy(() -> new Relation("r-1", "cliente", "e-cliente", null, true, null, null, null))
                .withMessageContaining("cardinality");
    }
}