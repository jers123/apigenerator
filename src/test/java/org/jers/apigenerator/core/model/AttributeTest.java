package org.jers.apigenerator.core.model;

import org.jers.apigenerator.testsupport.TestModels;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class AttributeTest {

    @Test
    void theNameIsStrippedAndABlankColumnMeansDerived() {
        Attribute attribute = new Attribute("a-1", "  nombre ", " ", AttributeType.of(ScalarType.STRING), true,
                false, null);

        assertThat(attribute.name()).isEqualTo("nombre");
        assertThat(attribute.columnName()).isNull();
    }

    @Test
    void aMissingNameBecomesEmptyForTheValidator() {
        Attribute attribute = new Attribute("a-1", null, null, AttributeType.of(ScalarType.STRING), false, false,
                null);

        assertThat(attribute.name()).isEmpty();
    }

    @Test
    void missingConstraintsMeanNone() {
        Attribute attribute = new Attribute("a-1", "nombre", null, AttributeType.of(ScalarType.STRING), true,
                false, null);

        assertThat(attribute.constraints()).isEqualTo(AttributeConstraints.none());
    }

    @Test
    void itNeedsAnInternalIdAndNamesTheAttributeInTheMessage() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> TestModels.attribute(null, "nombre", ScalarType.STRING, true))
                .withMessageContaining("nombre");
    }

    @Test
    void itNeedsAType() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Attribute("a-1", "nombre", null, null, true, false, null))
                .withMessageContaining("type");
    }

    @Test
    void theColumnIsDerivedInSnakeCaseUnlessWrittenByTheUser() {
        Attribute derived = TestModels.attribute("a-1", "fechaNacimiento", ScalarType.LOCAL_DATE, false);
        Attribute written = new Attribute("a-2", "fechaNacimiento", "nacimiento",
                AttributeType.of(ScalarType.LOCAL_DATE), false, false, null);

        assertThat(derived.effectiveColumnName()).isEqualTo("fecha_nacimiento");
        assertThat(written.effectiveColumnName()).isEqualTo("nacimiento");
    }
}