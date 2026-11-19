package org.jers.apigenerator.core.model;

import org.jers.apigenerator.testsupport.TestModels;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainModelTest {

    @Test
    void anEmptyModelUsesTheDefaults() {
        DomainModel model = new DomainModel(null, null, null, null);

        assertThat(model).isEqualTo(DomainModel.empty());
        assertThat(model.idSettings()).isEqualTo(IdSettings.defaults());
        assertThat(model.audit()).isEqualTo(AuditFields.none());
        assertThat(model.superclasses()).isEmpty();
        assertThat(model.entities()).isEmpty();
    }

    @Test
    void theShortConstructorHasNoSuperclasses() {
        assertThat(new DomainModel(null, null, TestModels.clientsAndOrders().entities()).superclasses()).isEmpty();
    }

    @Test
    void entitiesAndSuperclassesAreFoundByTheirInternalId() {
        DomainModel model = TestModels.inheritance();

        assertThat(model.findEntity("e-factura")).map(Entity::name).contains("Factura");
        assertThat(model.findEntity("e-nada")).isEmpty();
        assertThat(model.findSuperclass("s-documento")).map(Superclass::name).contains("Documento");
        assertThat(model.findSuperclass("s-nada")).isEmpty();
    }

    @Test
    void theLineageGoesFromTheSuperclassUpToTheRoot() {
        DomainModel model = TestModels.inheritance();

        assertThat(model.lineage("s-documento")).extracting(Superclass::name)
                .containsExactly("Documento", "Auditable");
        assertThat(model.lineage("s-auditable")).extracting(Superclass::name).containsExactly("Auditable");
    }

    @Test
    void theLineageIsEmptyWithoutSuperclassOrForAMissingOne() {
        DomainModel model = TestModels.inheritance();

        assertThat(model.lineage(null)).isEmpty();
        assertThat(model.lineage("s-nada")).isEmpty();
    }

    @Test
    void theLineageStopsAtAMissingParentOrACycle() {
        Superclass orphan = new Superclass("s-1", "Huerfana", "s-nada", List.of());
        Superclass a = new Superclass("s-a", "A", "s-b", List.of());
        Superclass b = new Superclass("s-b", "B", "s-a", List.of());
        DomainModel model = new DomainModel(null, null, List.of(orphan, a, b), List.of());

        assertThat(model.lineage("s-1")).extracting(Superclass::id).containsExactly("s-1");
        assertThat(model.lineage("s-a")).extracting(Superclass::id).containsExactly("s-a", "s-b");
    }
}