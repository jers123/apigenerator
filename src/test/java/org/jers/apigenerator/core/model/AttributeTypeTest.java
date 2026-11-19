package org.jers.apigenerator.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AttributeTypeTest {

    @Test
    void aScalarTypeDoesNotUseAnEnum() {
        AttributeType type = AttributeType.of(ScalarType.LONG);

        assertThat(type.scalar()).isEqualTo(ScalarType.LONG);
        assertThat(type.usesEnum()).isFalse();
    }

    @Test
    void anEnumTypePointsToTheEnumOfTheModel() {
        AttributeType type = new AttributeType(null, " en-estado ");

        assertThat(type.enumId()).isEqualTo("en-estado");
        assertThat(type.usesEnum()).isTrue();
    }

    @Test
    void itIsScalarOrEnumButNeverNeitherNorBoth() {
        assertThatIllegalArgumentException().isThrownBy(() -> new AttributeType(null, " "));
        assertThatIllegalArgumentException().isThrownBy(() -> new AttributeType(null, null));
        assertThatIllegalArgumentException().isThrownBy(() -> new AttributeType(ScalarType.STRING, "en-estado"));
    }
}