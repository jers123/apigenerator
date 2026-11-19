package org.jers.apigenerator.application.model;

import org.jers.apigenerator.core.model.DomainModel;
import org.jers.apigenerator.core.model.Entity;
import org.jers.apigenerator.core.project.ModelDependencyGroups;
import org.jers.apigenerator.testsupport.TestModels;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ModelServiceTest {

	private final ModelService service = new ModelService("spring-data", "validation");

	@Test
	void validatesWithTheCoreRules() {
		assertThat(service.validate(TestModels.clientsAndOrders()).issues()).isEmpty();
		assertThat(service.validate(new DomainModel(null, null, List.of(
				new Entity("e-1", "cliente", null, null, null, List.of(), List.of())))).hasErrors()).isTrue();
	}

	@Test
	void derivesInversesAndMappings() {
		var derivations = service.derive(TestModels.clientsAndOrders());

		assertThat(derivations.inversesOf("e-cliente")).hasSize(1);
		assertThat(derivations.mappingOf("r-cliente")).isPresent();
	}

	@Test
	void reportsTheConfiguredDependencyGroups() {
		assertThat(service.dependencyGroups()).isEqualTo(ModelDependencyGroups.defaults());
		assertThat(new ModelService("jpa", "bean-validation").dependencyGroups())
				.isEqualTo(new ModelDependencyGroups("jpa", "bean-validation"));
	}

	@Test
	void invalidGroupIdsStopTheStartup() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ModelService("Spring Data", "validation"))
				.withMessageContaining("persistenceGroupId");
	}
}