package org.jers.apigenerator.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class InverseRelationTest {

    @Test
    void keepsTheNameTheSourceAndTheCardinalitySeenFromTheTarget() {
        InverseRelation inverse = new InverseRelation("pedidos", "e-pedido", "r-cliente", Cardinality.ONE_TO_MANY);

        assertThat(inverse.name()).isEqualTo("pedidos");
        assertThat(inverse.cardinality()).isEqualTo(Cardinality.ONE_TO_MANY);
    }

    @Test
    void needsAllItsData() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new InverseRelation(" ", "e-pedido", "r-cliente", Cardinality.ONE_TO_MANY));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new InverseRelation("pedidos", null, "r-cliente", Cardinality.ONE_TO_MANY));
        assertThatNullPointerException()
                .isThrownBy(() -> new InverseRelation("pedidos", "e-pedido", "r-cliente", null));
    }
}