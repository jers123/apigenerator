package org.jers.apigenerator.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class IdSettingsTest {

	@Test
	void theDefaultsAreLongWithIdentityInEachEntity() {
		assertThat(IdSettings.defaults())
				.isEqualTo(new IdSettings(ScalarType.LONG, IdGeneration.IDENTITY, IdPlacement.ENTITY));
	}

	@Test
	void withoutPlacementTheIdGoesInEachEntity() {
		assertThat(new IdSettings(ScalarType.UUID, IdGeneration.UUID).placement()).isEqualTo(IdPlacement.ENTITY);
		assertThat(new IdSettings(ScalarType.UUID, IdGeneration.UUID, null).placement())
				.isEqualTo(IdPlacement.ENTITY);
	}

	@Test
	void typeAndGenerationAreRequired() {
		assertThatNullPointerException().isThrownBy(() -> new IdSettings(null, IdGeneration.IDENTITY));
		assertThatNullPointerException().isThrownBy(() -> new IdSettings(ScalarType.LONG, null));
	}
}