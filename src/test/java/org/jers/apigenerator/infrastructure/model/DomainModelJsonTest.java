package org.jers.apigenerator.infrastructure.model;

import org.jers.apigenerator.core.model.Attribute;
import org.jers.apigenerator.core.model.AttributeType;
import org.jers.apigenerator.core.model.AttributeConstraints;
import org.jers.apigenerator.core.model.AuditFields;
import org.jers.apigenerator.core.model.DomainModel;
import org.jers.apigenerator.core.model.Entity;
import org.jers.apigenerator.core.model.IdGeneration;
import org.jers.apigenerator.core.model.IdPlacement;
import org.jers.apigenerator.core.model.IdSettings;
import org.jers.apigenerator.core.model.ScalarType;
import org.jers.apigenerator.core.model.Superclass;
import org.jers.apigenerator.core.model.TemporalRule;
import org.jers.apigenerator.testsupport.TestModels;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainModelJsonTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static DomainModel roundTrip(final DomainModel model) {
        return MAPPER.readValue(MAPPER.writeValueAsString(model), DomainModel.class);
    }

    @Test
    void aModelWithRelationsSurvivesAWriteAndReadRoundTrip() {
        DomainModel model = TestModels.clientsAndOrders();

        assertThat(roundTrip(model)).isEqualTo(model);
    }

    @Test
    void anInheritanceTreeSurvivesTheRoundTrip() {
        DomainModel model = TestModels.inheritance();

        assertThat(roundTrip(model)).isEqualTo(model);
    }

    @Test
    void idSettingsAuditSuperclassesAndConstraintsSurviveTheRoundTrip() {
        Attribute active = new Attribute("a-activo", "activo", null, AttributeType.of(ScalarType.BOOLEAN), true,
                false, null);
        Attribute birth = new Attribute("a-nac", "fechaNacimiento", "nacimiento",
                AttributeType.of(ScalarType.LOCAL_DATE), false, false,
                new AttributeConstraints(null, null, null, null, null, null, null, false, TemporalRule.PAST));
        Attribute price = new Attribute("a-precio", "precio", null, AttributeType.of(ScalarType.BIG_DECIMAL), true,
                false, new AttributeConstraints(null, null, BigDecimal.ZERO, new BigDecimal("999.99"), 5, 2, null,
                false, null));
        DomainModel model = new DomainModel(
                new IdSettings(ScalarType.UUID, IdGeneration.UUID, IdPlacement.ROOT_SUPERCLASS),
                new AuditFields(true, true),
                List.of(new Superclass("s-1", "Auditable", null, List.of(active))),
                List.of(new Entity("e-1", "Persona", null, null, null, "s-1", List.of(birth, price), List.of())));

        assertThat(roundTrip(model)).isEqualTo(model);
    }

    @Test
    void theJsonHasNoExtraPropertiesFromHelperMethods() {
        String json = MAPPER.writeValueAsString(TestModels.inheritance());

        assertThat(json).doesNotContain("\"bidirectional\"", "\"usesEnum\"", "\"hasAny\"", "\"any\"",
                "\"lineage\"");
    }
}