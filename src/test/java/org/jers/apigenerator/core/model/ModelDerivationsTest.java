package org.jers.apigenerator.core.model;

import org.jers.apigenerator.testsupport.TestModels;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelDerivationsTest {

    private static Entity entity(final String id, final String name, final Relation... relations) {
        return new Entity(id, name, null, null, null, List.of(), List.of(relations));
    }

    private static Relation relation(final String id, final String name, final String target,
                                     final Cardinality cardinality, final String inverse) {
        return new Relation(id, name, target, cardinality, false, inverse, null, null);
    }

    private static ModelDerivations derive(final Entity... entities) {
        return new ModelDerivations(new DomainModel(null, null, List.of(entities)));
    }

    @Test
    void aBidirectionalManyToOneCreatesTheInverseCollectionInTheTarget() {
        ModelDerivations derivations = new ModelDerivations(TestModels.clientsAndOrders());

        assertThat(derivations.inversesOf("e-cliente"))
                .containsExactly(new InverseRelation("pedidos", "e-pedido", "r-cliente", Cardinality.ONE_TO_MANY));
        assertThat(derivations.inversesOf("e-pedido")).isEmpty();
    }

    @Test
    void manyToOneKeepsTheForeignKeyInTheDeclaringTable() {
        RelationMapping mapping = new ModelDerivations(TestModels.clientsAndOrders()).mappingOf("r-cliente")
                .orElseThrow();

        assertThat(mapping).isEqualTo(new RelationMapping("r-cliente", "e-pedido", "cliente_id", null, null));
    }

    @Test
    void oneToManyPutsTheForeignKeyInTheTargetNamedAfterTheInverse() {
        ModelDerivations derivations = derive(
                entity("e-cliente", "Cliente",
                        relation("r-pedidos", "pedidos", "e-pedido", Cardinality.ONE_TO_MANY, "comprador")),
                entity("e-pedido", "Pedido"));

        assertThat(derivations.mappingOf("r-pedidos").orElseThrow())
                .isEqualTo(new RelationMapping("r-pedidos", "e-pedido", "comprador_id", null, null));
        assertThat(derivations.inversesOf("e-pedido")).extracting(InverseRelation::cardinality)
                .containsExactly(Cardinality.MANY_TO_ONE);
    }

    @Test
    void aUnidirectionalOneToManyNamesTheForeignKeyAfterTheDeclaringTable() {
        ModelDerivations derivations = derive(
                entity("e-cliente", "Cliente",
                        relation("r-pedidos", "pedidos", "e-pedido", Cardinality.ONE_TO_MANY, null)),
                entity("e-pedido", "Pedido"));

        assertThat(derivations.mappingOf("r-pedidos").orElseThrow().joinColumn()).isEqualTo("cliente_id");
    }

    @Test
    void manyToManyUsesAJoinTableOwnedByTheDeclaringEntity() {
        ModelDerivations derivations = derive(
                entity("e-cliente", "Cliente",
                        relation("r-favoritos", "favoritos", "e-producto", Cardinality.MANY_TO_MANY, "fans")),
                entity("e-producto", "Producto"));

        assertThat(derivations.mappingOf("r-favoritos").orElseThrow()).isEqualTo(new RelationMapping(
                "r-favoritos", "e-cliente", "cliente_id", "cliente_producto", "producto_id"));
    }

    @Test
    void aSelfManyToManyNamesTheSecondColumnAfterTheRelation() {
        ModelDerivations derivations = derive(
                entity("e-usuario", "Usuario",
                        relation("r-amigos", "amigos", "e-usuario", Cardinality.MANY_TO_MANY, null)));

        assertThat(derivations.mappingOf("r-amigos").orElseThrow()).isEqualTo(new RelationMapping(
                "r-amigos", "e-usuario", "usuario_id", "usuario_amigos", "amigos_id"));
    }

    @Test
    void writtenNamesWinOverDerivedOnes() {
        Relation favorites = new Relation("r-favoritos", "favoritos", "e-producto", Cardinality.MANY_TO_MANY, false,
                null, "id_cliente", "favoritos_cliente");
        ModelDerivations derivations = derive(entity("e-cliente", "Cliente", favorites),
                entity("e-producto", "Producto"));

        RelationMapping mapping = derivations.mappingOf("r-favoritos").orElseThrow();
        assertThat(mapping.joinColumn()).isEqualTo("id_cliente");
        assertThat(mapping.joinTable()).isEqualTo("favoritos_cliente");
        assertThat(mapping.inverseJoinColumn()).isEqualTo("producto_id");
    }

    @Test
    void aRelationToAMissingEntityIsSkippedForTheValidatorToReport() {
        ModelDerivations derivations = derive(
                entity("e-pedido", "Pedido",
                        relation("r-cliente", "cliente", "e-nada", Cardinality.MANY_TO_ONE, "pedidos")));

        assertThat(derivations.mappingOf("r-cliente")).isEmpty();
        assertThat(derivations.inversesOf("e-nada")).isEmpty();
    }
}