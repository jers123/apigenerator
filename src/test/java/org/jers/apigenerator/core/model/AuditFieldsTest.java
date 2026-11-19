package org.jers.apigenerator.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditFieldsTest {

	@Test
	void noneHasNoFields() {
		assertThat(AuditFields.none().any()).isFalse();
	}

	@Test
	void anyFieldTurnsItOn() {
		assertThat(new AuditFields(true, false).any()).isTrue();
		assertThat(new AuditFields(false, true).any()).isTrue();
	}
}