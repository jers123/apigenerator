package org.jers.apigenerator.core.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DependencyGroupTest {

    @Test
    void emptyGroupIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DependencyGroup("vacio", "Vacío", DependencyCategory.OTHER, List.of(), false))
                .withMessageContaining("Vacío");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Spring Web", "spring_web", "SpringWeb", "-web", "web-"})
    void invalidIdIsRejected(final String id) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DependencyGroup(id, "Spring Web", DependencyCategory.SPRING,
                        List.of(CatalogFixtures.library("Web")), false))
                .withMessageContaining("id");
    }

    @Test
    void groupIsNotAffectedByChangesToTheOriginalList() {
        List<Dependency> source = new ArrayList<>(List.of(CatalogFixtures.library("Web")));
        DependencyGroup group = new DependencyGroup("spring-web", "Spring Web", DependencyCategory.SPRING, source, true);

        source.add(CatalogFixtures.library("Otra"));

        assertThat(group.dependencies()).hasSize(1);
    }

    @Test
    void dependenciesCannotBeModifiedFromOutside() {
        DependencyGroup group = new DependencyGroup("spring-web", "Spring Web", DependencyCategory.SPRING,
                List.of(CatalogFixtures.library("Web")), true);

        assertThatThrownBy(() -> group.dependencies().add(CatalogFixtures.library("Otra")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void defaultDependenciesExcludeConditionalOnes() {
        Dependency jpa = CatalogFixtures.library("Data JPA");
        Dependency r2dbc = CatalogFixtures.reactiveLibrary("Data R2DBC");
        DependencyGroup group = new DependencyGroup("spring-data", "Spring Data", DependencyCategory.SPRING,
                List.of(jpa, r2dbc), false);

        assertThat(group.defaultDependencies()).containsExactly(jpa);
    }
}