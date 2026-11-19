package org.jers.apigenerator.core.project;

import org.jers.apigenerator.core.catalog.Dependency;
import org.jers.apigenerator.core.catalog.DependencyCatalog;
import org.jers.apigenerator.core.catalog.DependencyCategory;
import org.jers.apigenerator.core.catalog.DependencyGroup;
import org.jers.apigenerator.core.database.DatabaseEngine;
import org.jers.apigenerator.core.database.DatabaseKind;
import org.jers.apigenerator.core.model.AuditFields;
import org.jers.apigenerator.core.model.DomainModel;
import org.jers.apigenerator.core.model.Entity;
import org.jers.apigenerator.core.model.IdGeneration;
import org.jers.apigenerator.core.model.IdPlacement;
import org.jers.apigenerator.core.model.IdSettings;
import org.jers.apigenerator.core.model.ScalarType;
import org.jers.apigenerator.core.model.Superclass;
import org.jers.apigenerator.core.security.SecuritySelection;
import org.jers.apigenerator.core.security.TokenIssuer;
import org.jers.apigenerator.testsupport.TestCatalogs;
import org.jers.apigenerator.testsupport.TestIdentityProviders;
import org.jers.apigenerator.testsupport.TestModels;
import org.jers.apigenerator.testsupport.TestSecurityCatalogs;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ProjectPlannerTest {

    private static final String PARENT = "org.springframework.boot:spring-boot-starter-parent";
    private static final String WEBMVC = "org.springframework.boot:spring-boot-starter-webmvc";
    private static final String SPRINGDOC = "org.springdoc:springdoc-openapi-starter-webmvc-ui";
    private static final String LOMBOK = "org.projectlombok:lombok";
    private static final String JPA = "spring-boot-starter-data-jpa";
    private static final String VALIDATION = "spring-boot-starter-validation";

    private final ProjectPlanner planner = new ProjectPlanner(TestCatalogs.project(), TestSecurityCatalogs.small(),
            TestIdentityProviders.small());

    private static ProjectSpec secured(final SecuritySelection security, final boolean reactive) {
        return new ProjectSpec(ProjectFixtures.metadata(), ProjectFixtures.build(reactive), Set.of("spring-boot"),
                null, null, Map.of(), ProjectFixtures.architecture(), ProjectFixtures.options(), security);
    }

    private static ProjectSpec spec(final Set<String> groupIds, final Map<String, String> versions) {
        return new ProjectSpec(ProjectFixtures.metadata(), ProjectFixtures.build(false), groupIds, null, null,
                versions, ProjectFixtures.architecture(), ProjectFixtures.options());
    }

    private static ProjectSpec modeled(final DomainModel model, final DatabaseConnection database) {
        return new ProjectSpec(ProjectFixtures.metadata(), ProjectFixtures.build(false), Set.of("spring-boot"),
                database, null, Map.of(), ProjectFixtures.architecture(), ProjectFixtures.options(),
                SecuritySelection.none(), model);
    }

    private static DatabaseConnection postgresWithSequences() {
        DatabaseEngine engine = new DatabaseEngine("postgresql", "PostgreSQL", "postgresql-driver", false, 5432,
                "postgres", "jdbc:postgresql://{host}:{port}/{database}", DatabaseKind.RELATIONAL, true);
        return new DatabaseConnection(engine, "localhost", 5432, "clientes", "postgres", "secreto");
    }

    private static DomainModel withSequences() {
        return new DomainModel(new IdSettings(ScalarType.LONG, IdGeneration.SEQUENCE), AuditFields.none(),
                TestModels.clientsAndOrders().entities());
    }

    private static List<String> artifactIds(final ProjectPlan plan) {
        return plan.dependencies().stream().map(planned -> planned.dependency().artifactId()).toList();
    }

    private static String versionOf(final ProjectPlan plan, final String coordinates) {
        return plan.dependencies().stream()
                .filter(planned -> planned.dependency().coordinates().equals(coordinates))
                .findFirst()
                .orElseThrow()
                .version();
    }

    // ── Dependencias y versiones ─────────────────────────────────────

    @Test
    void springBootVersionComesFromTheParent() {
        ProjectPlan plan = planner.plan(spec(Set.of("spring-boot", "spring-web"), Map.of()));

        assertThat(plan.springBootVersion()).isEqualTo("4.1.2");
    }

    @Test
    void springBootArtifactsAreManagedAndTheRestKeepTheCatalogVersion() {
        ProjectPlan plan = planner.plan(spec(Set.of("spring-boot", "spring-web", "springdoc", "lombok"), Map.of()));

        assertThat(versionOf(plan, WEBMVC)).isNull();
        assertThat(versionOf(plan, "org.springframework.boot:spring-boot-maven-plugin")).isNull();
        assertThat(versionOf(plan, SPRINGDOC)).isEqualTo("3.2.0");
        assertThat(versionOf(plan, "org.apache.maven:apache-maven")).isEqualTo("3.10.0");
        assertThat(versionOf(plan, LOMBOK)).as("sin versión en el catálogo: gestionada").isNull();
    }

    @Test
    void chosenVersionsReplaceTheCatalogOnes() {
        ProjectPlan plan = planner.plan(spec(Set.of("spring-boot", "springdoc"),
                Map.of(PARENT, "4.2.0", SPRINGDOC, "3.3.0")));

        assertThat(plan.springBootVersion()).isEqualTo("4.2.0");
        assertThat(versionOf(plan, SPRINGDOC)).isEqualTo("3.3.0");
    }

    @Test
    void aVersionForAnArtifactOutsideTheProjectIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> planner.plan(spec(Set.of("spring-boot"), Map.of(LOMBOK, "1.18.42"))))
                .withMessageContaining(LOMBOK);
    }

    @Test
    void aVersionForASpringBootArtifactIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> planner.plan(spec(Set.of("spring-boot", "spring-web"), Map.of(WEBMVC, "4.0.0"))))
                .withMessageContaining(WEBMVC);
    }

    @Test
    void aProjectWithoutTheParentIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> planner.plan(spec(Set.of("spring-web"), Map.of())))
                .withMessageContaining("Spring Boot");
    }

    // ── Bases de datos ───────────────────────────────────────────────

    @Test
    void theDriverOfTheDatabaseEntersThePlan() {
        ProjectSpec spec = new ProjectSpec(ProjectFixtures.metadata(), ProjectFixtures.build(false),
                Set.of("spring-boot"), ProjectFixtures.postgres(), null, Map.of(), ProjectFixtures.architecture(),
                ProjectFixtures.options());

        assertThat(artifactIds(planner.plan(spec))).contains("postgresql");
    }

    @Test
    void aGroupThatNeedsADatabaseIsRejectedWithoutOne() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> planner.plan(spec(Set.of("spring-boot", "spring-data"), Map.of())))
                .withMessageContaining("Spring Data");
    }

    @Test
    void aGroupThatNeedsADatabaseIsAcceptedWithOne() {
        ProjectSpec spec = new ProjectSpec(ProjectFixtures.metadata(), ProjectFixtures.build(false),
                Set.of("spring-boot", "spring-data"), ProjectFixtures.postgres(), null, Map.of(),
                ProjectFixtures.architecture(), ProjectFixtures.options());

        assertThat(artifactIds(planner.plan(spec))).contains(JPA, "postgresql");
    }

    @Test
    void aDriverGroupCannotBeChosenAsADependency() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> planner.plan(spec(Set.of("spring-boot", "postgresql-driver"), Map.of())))
                .withMessageContaining("PostgreSQL Driver");
    }

    // ── Seguridad ────────────────────────────────────────────────────

    @Test
    void securityArtifactsEnterThePlanOnceAndManaged() {
        ProjectPlan plan = planner.plan(secured(new SecuritySelection("http-basic", null, Set.of("cors"), null), false));

        assertThat(plan.dependencies())
                .filteredOn(planned -> planned.dependency().artifactId().equals("spring-boot-starter-security"))
                .singleElement()
                .satisfies(planned -> assertThat(planned.managed()).isTrue());
    }

    @Test
    void theOwnAuthorizationServerIsReportedAsASecondProject() {
        ProjectPlan plan = planner.plan(secured(new SecuritySelection("oauth2-resource-server", null, Set.of(),
                new TokenIssuer.OwnAuthorizationServer()), false));

        assertThat(plan.authorizationServerArtifactId()).isEqualTo("api-clientes-auth-server");
        assertThat(plan.security().issuerUri()).isEqualTo("http://localhost:9000");
    }

    @Test
    void withoutAnAuthorizationServerThereIsNoSecondProject() {
        assertThat(planner.plan(spec(Set.of("spring-boot"), Map.of())).authorizationServerArtifactId()).isNull();
    }

    @Test
    void theOwnAuthorizationServerIsBlockedInReactiveProjects() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> planner.plan(secured(new SecuritySelection("oauth2-resource-server", null, Set.of(),
                        new TokenIssuer.OwnAuthorizationServer()), true)))
                .withMessageContaining("reactivo");
    }

    @Test
    void aGroupWithSecurityArtifactsIsRejected() {
        Dependency starter = TestSecurityCatalogs.small().findMechanism("http-basic").orElseThrow()
                .dependencies().getFirst();
        DependencyCatalog catalog = TestCatalogs.project().withGroupAdded(new DependencyGroup("seguridad",
                "Seguridad", DependencyCategory.SPRING, List.of(starter), false));
        ProjectPlanner withSecurityGroup = new ProjectPlanner(catalog, TestSecurityCatalogs.small(),
                TestIdentityProviders.small());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> withSecurityGroup.plan(spec(Set.of("spring-boot", "seguridad"), Map.of())))
                .withMessageContaining("[Seguridad]");
    }

    // ── Modelo ───────────────────────────────────────────────────────

    @Test
    void withoutEntitiesTheModelAddsNoGroups() {
        ProjectPlan plan = planner.plan(modeled(DomainModel.empty(), ProjectFixtures.postgres()));

        assertThat(artifactIds(plan)).doesNotContain(JPA, VALIDATION);
    }

    @Test
    void entitiesWithADatabaseAddPersistenceAndRequiredFieldsAddValidation() {
        ProjectSpec spec = modeled(TestModels.clientsAndOrders(), ProjectFixtures.postgres());

        assertThat(planner.groupsRequiredByModel(spec)).containsExactly("spring-data", "validation");
        assertThat(artifactIds(planner.plan(spec))).contains(JPA, VALIDATION, "postgresql");
    }

    @Test
    void entitiesWithoutADatabaseLiveInMemoryWithoutPersistenceGroup() {
        ProjectSpec spec = modeled(TestModels.clientsAndOrders(), null);

        assertThat(planner.groupsRequiredByModel(spec)).containsExactly("validation");
        assertThat(artifactIds(planner.plan(spec))).doesNotContain(JPA).contains(VALIDATION);
    }

    @Test
    void optionalFieldsWithoutConstraintsDoNotNeedValidation() {
        Entity tag = new Entity("e-1", "Etiqueta", null, null, null,
                List.of(TestModels.attribute("a-1", "nombre", ScalarType.STRING, false)), List.of());
        ProjectSpec spec = modeled(new DomainModel(null, null, List.of(tag)), null);

        assertThat(planner.groupsRequiredByModel(spec)).isEmpty();
    }

    @Test
    void requiredAttributesOfASuperclassAlsoNeedValidation() {
        Superclass base = new Superclass("s-1", "Base", null,
                List.of(TestModels.attribute("a-1", "activo", ScalarType.BOOLEAN, true)));
        Entity tag = new Entity("e-1", "Etiqueta", null, null, null, "s-1",
                List.of(TestModels.attribute("a-2", "nombre", ScalarType.STRING, false)), List.of());
        DomainModel model = new DomainModel(new IdSettings(ScalarType.LONG, IdGeneration.IDENTITY,
                IdPlacement.ROOT_SUPERCLASS), AuditFields.none(), List.of(base), List.of(tag));

        assertThat(planner.groupsRequiredByModel(modeled(model, null))).containsExactly("validation");
    }

    @Test
    void anInheritanceTreeIsPlannedWithPersistenceAndValidation() {
        assertThat(artifactIds(planner.plan(modeled(TestModels.inheritance(), ProjectFixtures.postgres()))))
                .contains(JPA, VALIDATION);
    }

    @Test
    void aModelWithErrorsIsRejectedNamingWhereToFixIt() {
        Entity invalid = new Entity("e-1", "cliente", null, null, null,
                List.of(TestModels.attribute("a-1", "nombre", ScalarType.STRING, true)), List.of());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> planner.plan(modeled(new DomainModel(null, null, List.of(invalid)), null)))
                .withMessageContaining("1 error")
                .withMessageContaining("paso Modelo")
                .withMessageContaining("PascalCase");
    }

    @Test
    void sequencesNeedAnEngineThatHasThem() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> planner.plan(modeled(withSequences(), ProjectFixtures.postgres())))
                .withMessageContaining("Secuencia")
                .withMessageContaining("PostgreSQL");

        assertThat(artifactIds(planner.plan(modeled(withSequences(), postgresWithSequences())))).contains(JPA);
    }

    @Test
    void sequencesWithoutADatabaseAreSimulatedInMemory() {
        assertThat(planner.plan(modeled(withSequences(), null)).dependencies()).isNotEmpty();
    }

    @Test
    void aMissingModelGroupNamesItsProperty() {
        ProjectPlanner misconfigured = new ProjectPlanner(TestCatalogs.project(), TestSecurityCatalogs.small(),
                TestIdentityProviders.small(), new ModelDependencyGroups("spring-data", "no-existe"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> misconfigured.plan(modeled(TestModels.clientsAndOrders(), null)))
                .withMessageContaining("no-existe")
                .withMessageContaining("app.model.validation-group");
    }
}