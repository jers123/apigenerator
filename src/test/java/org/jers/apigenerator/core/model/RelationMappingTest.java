package org.jers.apigenerator.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RelationMappingTest {

    @Test
    void aForeignKeyMappingHasNoJoinTable() {
        RelationMapping mapping = new RelationMapping("r-1", "e-pedido", "cliente_id", null, null);

        assertThat(mapping.usesJoinTable()).isFalse();
    }

    @Test
    void aJoinTableComesWithItsSecondColumn() {
        RelationMapping mapping = new RelationMapping("r-1", "e-cliente", "cliente_id", "cliente_producto",
                "producto_id");

        assertThat(mapping.usesJoinTable()).isTrue();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RelationMapping("r-1", "e-cliente", "cliente_id", "cliente_producto", null));
    }

    @Test
    void needsTheRelationTheOwnerAndTheColumn() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RelationMapping("r-1", "e-pedido", null, null, null));
    }
}