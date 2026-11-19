package org.jers.apigenerator.core.model;

import org.jers.apigenerator.testsupport.TestModels;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SuperclassTest {

	@Test
	void aBlankParentMeansRootAndMissingAttributesMeanEmpty() {
		Superclass root = new Superclass("s-1", " Auditable ", " ", null);

		assertThat(root.name()).isEqualTo("Auditable");
		assertThat(root.parentId()).isNull();
		assertThat(root.attributes()).isEmpty();
	}

	@Test
	void itNeedsAnInternalIdAndNamesTheSuperclassInTheMessage() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new Superclass(null, "Auditable", null, List.of()))
				.withMessageContaining("Auditable");
	}

	@Test
	void attributesCannotBeChangedFromOutside() {
		List<Attribute> attributes = new ArrayList<>(List.of(TestModels.attribute("a-1", "activo",
				ScalarType.BOOLEAN, true)));
		Superclass superclass = new Superclass("s-1", "Auditable", null, attributes);

		attributes.clear();

		assertThat(superclass.attributes()).hasSize(1);
	}
}