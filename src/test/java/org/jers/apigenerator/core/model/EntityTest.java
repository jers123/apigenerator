package org.jers.apigenerator.core.model;

import org.jers.apigenerator.testsupport.TestModels;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityTest {

    private static Entity entity(final String name, final String table, final String path, final String sequence) {
        return new Entity("e-1", name, table, path, sequence, List.of(), List.of());
    }

    @Test
    void blankNamesMeanDerivedAndMissingListsMeanEmpty() {
        Entity entity = new Entity("e-1", null, " ", "", null, " ", null, null);

        assertThat(entity.name()).isEmpty();
        assertThat(entity.tableName()).isNull();
        assertThat(entity.resourcePath()).isNull();
        assertThat(entity.sequenceName()).isNull();
        assertThat(entity.superclassId()).isNull();
        assertThat(entity.attributes()).isEmpty();
        assertThat(entity.relations()).isEmpty();
    }

    @Test
    void withoutSuperclassTheShortConstructorLeavesItEmpty() {
        assertThat(entity("Cliente", null, null, null).superclassId()).isNull();
    }

    @Test
    void theSuperclassIsKept() {
        Entity invoice = TestModels.inheritance().findEntity("e-factura").orElseThrow();

        assertThat(invoice.superclassId()).isEqualTo("s-documento");
    }

    @Test
    void itNeedsAnInternalIdAndNamesTheEntityInTheMessage() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Entity(" ", "Cliente", null, null, null, List.of(), List.of()))
                .withMessageContaining("Cliente");
    }

    @Test
    void listsCannotBeChangedFromOutside() {
        List<Attribute> attributes = new ArrayList<>(List.of(TestModels.attribute("a-1", "nombre",
                ScalarType.STRING, true)));
        Entity entity = new Entity("e-1", "Cliente", null, null, null, attributes, List.of());

        attributes.clear();

        assertThat(entity.attributes()).hasSize(1);
        assertThatThrownBy(() -> entity.attributes().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void relationsAreFoundByTheirInternalId() {
        Entity order = TestModels.clientsAndOrders().findEntity("e-pedido").orElseThrow();

        assertThat(order.findRelation("r-cliente")).map(Relation::name).contains("cliente");
        assertThat(order.findRelation("r-nada")).isEmpty();
    }

    @Test
    void tablePathAndSequenceAreDerivedFromTheName() {
        Entity entity = entity("DetallePedido", null, null, null);

        assertThat(entity.effectiveTableName()).isEqualTo("detalle_pedido");
        assertThat(entity.effectiveResourcePath()).isEqualTo("/detalle-pedidos");
        assertThat(entity.effectiveSequenceName()).isEqualTo("detalle_pedido_seq");
    }

    @Test
    void writtenNamesWinOverDerivedOnes() {
        Entity entity = entity("Pais", "paises_tbl", "/paises", "seq_pais");

        assertThat(entity.effectiveTableName()).isEqualTo("paises_tbl");
        assertThat(entity.effectiveResourcePath()).isEqualTo("/paises");
        assertThat(entity.effectiveSequenceName()).isEqualTo("seq_pais");
    }

    @Test
    void theDerivedSequenceFollowsAWrittenTable() {
        assertThat(entity("Cliente", "clientes", null, null).effectiveSequenceName()).isEqualTo("clientes_seq");
    }
}