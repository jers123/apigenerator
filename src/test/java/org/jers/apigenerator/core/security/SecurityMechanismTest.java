package org.jers.apigenerator.core.security;

import org.jers.apigenerator.testsupport.TestSecurityCatalogs;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SecurityMechanismTest {

    private static final SecurityMechanism BASIC = TestSecurityCatalogs.mechanism("http-basic", "HTTP Basic",
            SecurityRole.API_AUTHENTICATION, false, Set.of());

    private static SecurityMechanism copy(final String id, final String description, final SecurityRole role,
                                          final boolean requiresIssuer, final Set<String> conflicts) {
        return new SecurityMechanism(id, BASIC.name(), description, role, BASIC.dependencies(),
                true, requiresIssuer, false, conflicts);
    }

    @Test
    void aMissingDescriptionBecomesEmpty() {
        assertThat(copy("http-basic", null, SecurityRole.API_AUTHENTICATION, false, Set.of()).description()).isEmpty();
    }

    @Test
    void theIdMustBeASlug() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> copy("HTTP Basic", "", SecurityRole.API_AUTHENTICATION, false, Set.of()))
                .withMessageContaining("id");
    }

    @Test
    void aMechanismNeedsAtLeastOneDependency() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SecurityMechanism("cors", "CORS", "", SecurityRole.ADD_ON, List.of(),
                        true, false, false, Set.of()))
                .withMessageContaining("dependencias");
    }

    @Test
    void onlyApiAuthenticationAndLoginMayRequireAnIssuer() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> copy("cors", "", SecurityRole.ADD_ON, true, Set.of()))
                .withMessageContaining("emisor");
        assertThat(copy("oidc-login", "", SecurityRole.BROWSER_LOGIN, true, Set.of()).requiresIssuer()).isTrue();
    }

    @Test
    void aMechanismCannotConflictWithItself() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> copy("http-basic", "", SecurityRole.API_AUTHENTICATION, false, Set.of("http-basic")))
                .withMessageContaining("consigo mismo");
    }

    @Test
    void conflictsCannotBeChangedFromOutside() {
        Set<String> conflicts = new HashSet<>(Set.of("form-login"));
        SecurityMechanism mechanism = copy("http-basic", "", SecurityRole.API_AUTHENTICATION, false, conflicts);

        conflicts.add("api-key");

        assertThat(mechanism.conflictsWith()).containsExactly("form-login");
    }
}