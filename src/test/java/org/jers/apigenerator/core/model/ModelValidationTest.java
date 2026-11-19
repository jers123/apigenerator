package org.jers.apigenerator.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelValidationTest {

	private static final ModelIssue ERROR = new ModelIssue(IssueSeverity.ERROR, "e-1", "Cliente", "Error");
	private static final ModelIssue WARNING = new ModelIssue(IssueSeverity.WARNING, "e-1", "Cliente", "Aviso");

	@Test
	void onlyErrorsBlockTheProject() {
		assertThat(new ModelValidation(List.of(WARNING)).hasErrors()).isFalse();
		assertThat(new ModelValidation(List.of(WARNING, ERROR)).hasErrors()).isTrue();
		assertThat(new ModelValidation(List.of()).hasErrors()).isFalse();
	}

	@Test
	void separatesErrorsFromWarnings() {
		ModelValidation validation = new ModelValidation(List.of(ERROR, WARNING));

		assertThat(validation.errors()).containsExactly(ERROR);
		assertThat(validation.warnings()).containsExactly(WARNING);
	}
}