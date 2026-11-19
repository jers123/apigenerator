package org.jers.apigenerator.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReservedWordsTest {

	@Test
	void recognizesJavaKeywordsCaseSensitively() {
		assertThat(ReservedWords.javaKeyword("class")).isTrue();
		assertThat(ReservedWords.javaKeyword("record")).isTrue();
		assertThat(ReservedWords.javaKeyword("Class")).isFalse();
		assertThat(ReservedWords.javaKeyword("nombre")).isFalse();
	}

	@Test
	void recognizesSqlWordsIgnoringCase() {
		assertThat(ReservedWords.sqlReserved("order")).isTrue();
		assertThat(ReservedWords.sqlReserved("USER")).isTrue();
		assertThat(ReservedWords.sqlReserved("pedido")).isFalse();
	}

	@Test
	void recognizesClassesUsedByTheGeneratedCode() {
		assertThat(ReservedWords.reservedClassName("String")).isTrue();
		assertThat(ReservedWords.reservedClassName("Entity")).isTrue();
		assertThat(ReservedWords.reservedClassName("Cliente")).isFalse();
	}
}