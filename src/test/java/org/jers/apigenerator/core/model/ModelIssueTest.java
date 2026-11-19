package org.jers.apigenerator.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ModelIssueTest {
	@Test
	void aGeneralIssueHasNoElement() {
		ModelIssue issue = new ModelIssue(IssueSeverity.ERROR, null, "Id", "Mensaje");

		assertThat(issue.elementId()).isNull();
	}

	@Test
	void needsSeverityLocationAndMessage() {
		assertThatNullPointerException().isThrownBy(() -> new ModelIssue(null, null, "Id", "Mensaje"));
		assertThatIllegalArgumentException().isThrownBy(() -> new ModelIssue(IssueSeverity.ERROR, null, " ", "M"));
		assertThatIllegalArgumentException().isThrownBy(() -> new ModelIssue(IssueSeverity.ERROR, null, "Id", ""));
	}
}