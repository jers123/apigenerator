package org.jers.apigenerator.core.project;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ModelDependencyGroupsTest {

	@Test
	void theDefaultsAreTheFactoryGroups() {
		assertThat(ModelDependencyGroups.defaults())
				.isEqualTo(new ModelDependencyGroups("spring-data", "validation"));
	}

	@Test
	void groupIdsMustBeSlugs() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ModelDependencyGroups("Spring Data", "validation"))
				.withMessageContaining("persistenceGroupId");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ModelDependencyGroups("spring-data", " "))
				.withMessageContaining("validationGroupId");
	}
}