package org.jers.apigenerator.core.security;

import org.jers.apigenerator.testsupport.TestSecurityCatalogs;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityCatalogTest {

    private static SecurityMechanism mechanism(final String id, final String name, final SecurityRole role,
                                               final Set<String> conflicts) {
        return TestSecurityCatalogs.mechanism(id, name, role, false, conflicts);
    }

    @Test
    void anEmptyCatalogIsValid() {
        assertThat(new SecurityCatalog(List.of()).mechanisms()).isEmpty();
    }

    @Test
    void repeatedIdsAndNamesAreRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SecurityCatalog(List.of(
                        mechanism("cors", "CORS", SecurityRole.ADD_ON, Set.of()),
                        mechanism("cors", "Otro", SecurityRole.ADD_ON, Set.of()))))
                .withMessageContaining("cors");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SecurityCatalog(List.of(
                        mechanism("cors", "CORS", SecurityRole.ADD_ON, Set.of()),
                        mechanism("cors-2", "CORS", SecurityRole.ADD_ON, Set.of()))))
                .withMessageContaining("CORS");
    }

    @Test
    void aConflictWithAnUnknownMechanismIsRejectedNamingIt() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SecurityCatalog(List.of(
                        mechanism("form-login", "Formulario", SecurityRole.BROWSER_LOGIN, Set.of("kerberos")))))
                .withMessageContaining("kerberos");
    }

    @Test
    void atMostOneAuthorizationServerIsAllowed() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SecurityCatalog(List.of(
                        mechanism("server-a", "A", SecurityRole.AUTHORIZATION_SERVER, Set.of()),
                        mechanism("server-b", "B", SecurityRole.AUTHORIZATION_SERVER, Set.of()))))
                .withMessageContaining("Servidor de autorización");
    }

    @Test
    void conflictsCountInBothDirections() {
        SecurityCatalog catalog = TestSecurityCatalogs.small();

        assertThat(catalog.conflict("form-login", "http-basic")).isTrue();
        assertThat(catalog.conflict("http-basic", "form-login")).isTrue();
        assertThat(catalog.conflict("http-basic", "cors")).isFalse();
    }

    @Test
    void withRoleKeepsCatalogOrder() {
        assertThat(TestSecurityCatalogs.small().withRole(SecurityRole.API_AUTHENTICATION))
                .extracting(SecurityMechanism::id)
                .containsExactly("http-basic", "oauth2-resource-server");
    }

    @Test
    void anAddedMechanismGoesLastAndARepeatedIdFails() {
        SecurityCatalog next = TestSecurityCatalogs.small()
                .withMechanismAdded(mechanism("api-key", "API Key", SecurityRole.API_AUTHENTICATION, Set.of()));

        assertThat(next.mechanisms()).last().extracting(SecurityMechanism::id).isEqualTo("api-key");
        assertThatThrownBy(() -> next.withMechanismAdded(mechanism("api-key", "Otra", SecurityRole.ADD_ON, Set.of())))
                .isInstanceOf(DuplicateSecurityMechanismException.class);
    }

    @Test
    void aReplacedMechanismKeepsItsPositionAndAnUnknownOneFails() {
        SecurityCatalog next = TestSecurityCatalogs.small()
                .withMechanismReplaced(mechanism("cors", "CORS abierto", SecurityRole.ADD_ON, Set.of()));

        assertThat(next.mechanisms().get(3).name()).isEqualTo("CORS abierto");
        assertThatThrownBy(() -> next.withMechanismReplaced(mechanism("kerberos", "Kerberos", SecurityRole.ADD_ON,
                Set.of())))
                .isInstanceOf(SecurityMechanismNotFoundException.class);
    }

    @Test
    void replacingAMechanismCannotBreakAnotherOnesConflict() {
        SecurityCatalog catalog = TestSecurityCatalogs.small();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> catalog.withMechanismReplaced(
                        mechanism("form-login", "Formulario", SecurityRole.BROWSER_LOGIN, Set.of("no-existe"))))
                .withMessageContaining("no-existe");
    }
}