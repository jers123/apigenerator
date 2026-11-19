package org.jers.apigenerator.core.selection;

import org.jers.apigenerator.core.catalog.Dependency;
import org.jers.apigenerator.core.catalog.DependencyGroup;
import org.jers.apigenerator.testsupport.TestCatalogs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DependencySelectorTest {

    private final DependencySelector selector = new DependencySelector(TestCatalogs.small());

    @Test
    @DisplayName("R1: Maven incluye su grupo y descarta Gradle aunque se pida")
    void mavenForcesItsGroupAndDropsGradle() {
        ResolvedSelection result = selector.select(request(BuildTool.MAVEN, false, "gradle"));

        assertThat(groupIds(result)).containsExactly("maven");
    }

    @Test
    @DisplayName("R1: Gradle incluye su grupo y descarta Maven aunque se pida")
    void gradleForcesItsGroupAndDropsMaven() {
        ResolvedSelection result = selector.select(request(BuildTool.GRADLE, false, "maven"));

        assertThat(groupIds(result)).containsExactly("gradle");
    }

    @Test
    @DisplayName("R2: sin modo reactivo se usan las variantes tradicionales")
    void nonReactiveUsesDefaultDependencies() {
        ResolvedSelection result = selector.select(request(BuildTool.MAVEN, false, "spring-web"));

        assertThat(dependencyNames(result)).containsExactly("Maven Plugin", "WebMVC");
    }

    @Test
    @DisplayName("R3: en modo reactivo WebFlux reemplaza a WebMVC")
    void reactiveReplacesTraditionalVariants() {
        ResolvedSelection result = selector.select(request(BuildTool.MAVEN, true, "spring-web"));

        assertThat(dependencyNames(result)).containsExactly("Maven Plugin", "WebFlux");
    }

    @Test
    @DisplayName("R4: en modo reactivo la base de datos conserva JDBC y agrega R2DBC")
    void reactiveDatabaseKeepsJdbcAndAddsR2dbc() {
        ResolvedSelection result = selector.select(request(BuildTool.MAVEN, true, "postgresql"));

        assertThat(dependencyNames(result))
                .containsExactly("Maven Plugin", "PostgreSQL JDBC", "PostgreSQL R2DBC");
    }

    @Test
    @DisplayName("R5: el modo reactivo no altera grupos sin variantes reactivas")
    void reactiveDoesNotAffectGroupsWithoutReactiveVariants() {
        ResolvedSelection result = selector.select(request(BuildTool.MAVEN, true, "lombok"));

        assertThat(dependencyNames(result)).containsExactly("Maven Plugin", "Lombok");
    }

    @Test
    @DisplayName("R6: un grupo inexistente produce un error con su id")
    void unknownGroupIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> selector.select(request(BuildTool.MAVEN, false, "lombok", "kafka")))
                .withMessageContaining("kafka")
                .withMessageNotContaining("lombok");
    }

    @Test
    @DisplayName("R7: el resultado respeta el orden del catálogo, no el de la petición")
    void resultFollowsCatalogOrder() {
        ResolvedSelection result = selector.select(request(BuildTool.MAVEN, false, "postgresql", "lombok", "spring-web"));

        assertThat(groupIds(result)).containsExactly("maven", "spring-web", "lombok", "postgresql");
    }

    @Test
    @DisplayName("Los grupos no pedidos no aparecen")
    void unrequestedGroupsAreNotIncluded() {
        ResolvedSelection result = selector.select(request(BuildTool.MAVEN, false));

        assertThat(groupIds(result)).containsExactly("maven");
    }

    // ── utilidades del test ─────────────────────────────────────────

    private static SelectionRequest request(final BuildTool tool, final boolean reactive, final String... groupIds) {
        return new SelectionRequest(Set.of(groupIds), tool, reactive);
    }

    private static List<String> groupIds(final ResolvedSelection result) {
        return result.groups().stream().map(DependencyGroup::id).toList();
    }

    private static List<String> dependencyNames(final ResolvedSelection result) {
        return result.dependencies().stream().map(Dependency::name).toList();
    }
}