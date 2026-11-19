package org.jers.apigenerator.core.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ModelNamingTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "Cliente, cliente",
            "DetallePedido, detalle_pedido",
            "fechaNacimiento, fecha_nacimiento",
            "URLCorta, url_corta",
            "direccion2Envio, direccion2_envio",
            "id, id"})
    void snakeCaseSeparatesWordsAndAcronyms(final String name, final String expected) {
        assertThat(ModelNaming.snakeCase(name)).isEqualTo(expected);
    }

    @Test
    void kebabCaseUsesDashes() {
        assertThat(ModelNaming.kebabCase("DetallePedido")).isEqualTo("detalle-pedido");
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"cliente, clientes", "ciudad, ciudades", "luz, luces", "ley, leyes", "pais, paises"})
    void pluralFollowsTheSimpleSpanishRules(final String word, final String expected) {
        assertThat(ModelNaming.plural(word)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"Cliente, /clientes", "Ciudad, /ciudades", "DetallePedido, /detalle-pedidos", "Luz, /luces"})
    void resourcePathPluralizesOnlyTheLastWord(final String entity, final String expected) {
        assertThat(ModelNaming.resourcePath(entity)).isEqualTo(expected);
    }

    @Test
    void emptyOrMissingNamesGiveEmptyResults() {
        assertThat(ModelNaming.snakeCase(null)).isEmpty();
        assertThat(ModelNaming.plural("")).isEmpty();
        assertThat(ModelNaming.resourcePath("")).isEqualTo("/");
    }
}