package org.jers.apigenerator.core.project;

import org.jers.apigenerator.core.model.DomainModel;
import org.jers.apigenerator.core.security.SecuritySelection;
import org.jers.apigenerator.core.selection.BuildTool;
import org.jers.apigenerator.core.selection.SelectionRequest;
import org.jers.apigenerator.testsupport.TestModels;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ProjectSpecTest {

    private static ProjectSpec withVersions(final Map<String, String> versions) {
        return new ProjectSpec(ProjectFixtures.metadata(), ProjectFixtures.build(false), Set.of(), null, null,
                versions, ProjectFixtures.architecture(), ProjectFixtures.options());
    }

    private static ProjectSpec withDatabases(final DatabaseConnection database, final DatabaseConnection testDatabase,
                                             final boolean tests) {
        return new ProjectSpec(ProjectFixtures.metadata(), ProjectFixtures.build(false), Set.of(), database,
                testDatabase, Map.of(), ProjectFixtures.architecture(),
                new GenerationOptions(true, tests, ConfigFormat.PROPERTIES));
    }

    @Test
    void databaseDriversAreAddedToTheDependencySelection() {
        SelectionRequest request = ProjectFixtures.spec(ProjectFixtures.postgres(), ProjectFixtures.h2())
                .selectionRequest();

        assertThat(request.groupIds()).containsExactlyInAnyOrder("spring-web", "postgresql-driver", "h2-database");
    }

    @Test
    void withoutDatabasesOnlyTheChosenGroupsAreRequested() {
        SelectionRequest request = ProjectFixtures.spec(null, null).selectionRequest();

        assertThat(request.groupIds()).containsExactly("spring-web");
    }

    @Test
    void selectionCarriesBuildToolAndReactiveMode() {
        ProjectSpec spec = new ProjectSpec(ProjectFixtures.metadata(), ProjectFixtures.build(true),
                Set.of(), null, null, Map.of(), ProjectFixtures.architecture(), ProjectFixtures.options());

        assertThat(spec.selectionRequest().buildTool()).isEqualTo(BuildTool.MAVEN);
        assertThat(spec.selectionRequest().reactive()).isTrue();
    }

    @Test
    void databasesListsDevelopmentThenTest() {
        assertThat(ProjectFixtures.spec(ProjectFixtures.postgres(), ProjectFixtures.h2()).databases())
                .extracting(connection -> connection.engine().id())
                .containsExactly("postgresql", "h2");
        assertThat(ProjectFixtures.spec(ProjectFixtures.postgres(), null).databases())
                .extracting(connection -> connection.engine().id())
                .containsExactly("postgresql");
    }

    @Test
    void dependencyGroupsCannotBeChangedFromOutside() {
        Set<String> groupIds = new HashSet<>(Set.of("spring-web"));
        ProjectSpec spec = new ProjectSpec(ProjectFixtures.metadata(), ProjectFixtures.build(false),
                groupIds, null, null, Map.of(), ProjectFixtures.architecture(), ProjectFixtures.options());

        groupIds.add("lombok");

        assertThat(spec.dependencyGroupIds()).containsExactly("spring-web");
    }

    @Test
    void aTestDatabaseNeedsADevelopmentDatabase() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> withDatabases(null, ProjectFixtures.h2(), true))
                .withMessageContaining("base de desarrollo");
    }

    @Test
    void aTestDatabaseNeedsTestsThatUseIt() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> withDatabases(ProjectFixtures.postgres(), ProjectFixtures.h2(), false))
                .withMessageContaining("pruebas de integración");
    }

    @Test
    void withoutADevelopmentDatabaseTheDataLivesInMemory() {
        ProjectSpec spec = withDatabases(null, null, true);

        assertThat(spec.persistence()).isEqualTo(Persistence.IN_MEMORY);
        assertThat(spec.testPersistence()).isEqualTo(Persistence.IN_MEMORY);
    }

    @Test
    void withADevelopmentDatabaseButNoTestDatabaseTheTestsWorkInMemory() {
        ProjectSpec spec = withDatabases(ProjectFixtures.postgres(), null, true);

        assertThat(spec.persistence()).isEqualTo(Persistence.DATABASE);
        assertThat(spec.testPersistence()).isEqualTo(Persistence.IN_MEMORY);
    }

    @Test
    void withATestDatabaseTheTestsUseIt() {
        assertThat(withDatabases(ProjectFixtures.postgres(), ProjectFixtures.h2(), true).testPersistence())
                .isEqualTo(Persistence.DATABASE);
    }

    @Test
    void withoutTestsThereIsNoTestPersistence() {
        assertThat(withDatabases(ProjectFixtures.postgres(), null, false).testPersistence())
                .isEqualTo(Persistence.NONE);
    }

    @Test
    void versionKeysMustBeGroupIdColonArtifactId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> withVersions(Map.of("lombok", "1.18.42")))
                .withMessageContaining("groupId:artifactId");
    }

    @Test
    void blankVersionsAreRejectedNamingTheArtifact() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> withVersions(Map.of("org.projectlombok:lombok", " ")))
                .withMessageContaining("org.projectlombok:lombok");
    }

    @Test
    void versionsCannotBeChangedFromOutside() {
        Map<String, String> versions = new HashMap<>(Map.of("org.projectlombok:lombok", "1.18.42"));
        ProjectSpec spec = withVersions(versions);

        versions.put("com.google.code.gson:gson", "2.13.0");

        assertThat(spec.versionOverrides()).containsOnlyKeys("org.projectlombok:lombok");
    }

    @Test
    void withoutAModelTheProjectHasAnEmptyOne() {
        assertThat(withVersions(Map.of()).model()).isEqualTo(DomainModel.empty());
        assertThat(new ProjectSpec(ProjectFixtures.metadata(), ProjectFixtures.build(false), Set.of(), null, null,
                Map.of(), ProjectFixtures.architecture(), ProjectFixtures.options(), SecuritySelection.none(), null)
                .model()).isEqualTo(DomainModel.empty());
    }

    @Test
    void theModelIsKept() {
        DomainModel model = TestModels.clientsAndOrders();
        ProjectSpec spec = new ProjectSpec(ProjectFixtures.metadata(), ProjectFixtures.build(false), Set.of(), null,
                null, Map.of(), ProjectFixtures.architecture(), ProjectFixtures.options(), SecuritySelection.none(),
                model);

        assertThat(spec.model()).isSameAs(model);
    }
}