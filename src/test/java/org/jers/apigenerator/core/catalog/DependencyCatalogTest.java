package org.jers.apigenerator.core.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DependencyCatalogTest {
    private static DependencyGroup group(final String id, final String name, final DependencyCategory category) {
        return new DependencyGroup(id, name, category, List.of(CatalogFixtures.library(name)), false);
    }

    private static DependencyCatalog webAndLombok() {
        return new DependencyCatalog(List.of(
                group("spring-web", "Spring Web", DependencyCategory.SPRING),
                group("lombok", "Lombok", DependencyCategory.OTHER)));
    }

    @Test
    void duplicateGroupIdsAreRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DependencyCatalog(List.of(
                        group("web", "Spring Web", DependencyCategory.SPRING),
                        group("web", "Otro", DependencyCategory.SPRING))))
                .withMessageContaining("web");
    }

    @Test
    void duplicateGroupNamesAreRejectedEvenWithDifferentIds() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DependencyCatalog(List.of(
                        group("web", "Spring Web", DependencyCategory.SPRING),
                        group("web2", "Spring Web", DependencyCategory.SPRING))))
                .withMessageContaining("Spring Web");
    }

    @Test
    void unknownGroupIsEmptyNotNull() {
        DependencyCatalog catalog = new DependencyCatalog(List.of(group("lombok", "Lombok", DependencyCategory.OTHER)));

        assertThat(catalog.findGroup("no-existe")).isEmpty();
    }

    @Test
    void findGroupSearchesByIdNotByName() {
        DependencyCatalog catalog = new DependencyCatalog(List.of(group("lombok", "Lombok", DependencyCategory.OTHER)));

        assertThat(catalog.findGroup("lombok")).isPresent();
        assertThat(catalog.findGroup("Lombok")).isEmpty();
    }

    @Test
    void groupsInKeepsCatalogOrder() {
        DependencyCatalog catalog = new DependencyCatalog(List.of(
                group("mysql-driver", "MySQL Driver", DependencyCategory.DATABASE),
                group("lombok", "Lombok", DependencyCategory.OTHER),
                group("h2-database", "H2 Database", DependencyCategory.DATABASE)));

        assertThat(catalog.groupsIn(DependencyCategory.DATABASE))
                .extracting(DependencyGroup::id)
                .containsExactly("mysql-driver", "h2-database");
    }

    @Test
    void addedGroupGoesLastAndTheOriginalDoesNotChange() {
        DependencyCatalog original = webAndLombok();

        DependencyCatalog next = original.withGroupAdded(group("kafka", "Kafka", DependencyCategory.SPRING));

        assertThat(next.groups()).extracting(DependencyGroup::id).containsExactly("spring-web", "lombok", "kafka");
        assertThat(original.groups()).hasSize(2);
    }

    @Test
    void addingAnExistingIdFailsWithDuplicateGroup() {
        assertThatThrownBy(() -> webAndLombok().withGroupAdded(group("lombok", "Otro", DependencyCategory.OTHER)))
                .isInstanceOf(DuplicateDependencyGroupException.class)
                .hasMessageContaining("lombok");
    }

    @Test
    void addingAnExistingNameFailsWithValidationError() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> webAndLombok().withGroupAdded(group("lombok-2", "Lombok", DependencyCategory.OTHER)))
                .withMessageContaining("Lombok");
    }

    @Test
    void replacedGroupKeepsItsPosition() {
        DependencyCatalog next = webAndLombok()
                .withGroupReplaced(group("spring-web", "Spring Web MVC", DependencyCategory.SPRING));

        assertThat(next.groups()).extracting(DependencyGroup::name).containsExactly("Spring Web MVC", "Lombok");
    }

    @Test
    void replacingAnUnknownGroupFailsWithNotFound() {
        assertThatThrownBy(() -> webAndLombok().withGroupReplaced(group("kafka", "Kafka", DependencyCategory.SPRING)))
                .isInstanceOf(DependencyGroupNotFoundException.class)
                .hasMessageContaining("kafka");
    }

    @Test
    void renamingToTheNameOfAnotherGroupFails() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> webAndLombok()
                        .withGroupReplaced(group("lombok", "Spring Web", DependencyCategory.OTHER)))
                .withMessageContaining("Spring Web");
    }

    @Test
    void removedGroupDisappearsKeepingTheOrderOfTheRest() {
        DependencyCatalog next = webAndLombok()
                .withGroupAdded(group("kafka", "Kafka", DependencyCategory.SPRING))
                .withoutGroup("lombok");

        assertThat(next.groups()).extracting(DependencyGroup::id).containsExactly("spring-web", "kafka");
    }

    @Test
    void removingAnUnknownGroupFailsWithNotFound() {
        assertThatThrownBy(() -> webAndLombok().withoutGroup("kafka"))
                .isInstanceOf(DependencyGroupNotFoundException.class);
    }
}