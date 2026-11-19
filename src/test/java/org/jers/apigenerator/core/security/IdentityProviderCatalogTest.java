package org.jers.apigenerator.core.security;

import org.jers.apigenerator.testsupport.TestIdentityProviders;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityProviderCatalogTest {

    private static IdentityProvider google(final String id, final String name) {
        return new IdentityProvider(id, name, null, ProviderHosting.CLOUD, "https://accounts.google.com", List.of(),
                false, true, null);
    }

    @Test
    void repeatedIdsAndNamesAreRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new IdentityProviderCatalog(List.of(google("google", "Google"), google("google", "Otro"))))
                .withMessageContaining("google");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new IdentityProviderCatalog(List.of(google("google", "Google"), google("google-2", "Google"))))
                .withMessageContaining("Google");
    }

    @Test
    void anAddedProviderGoesLastAndARepeatedIdFails() {
        IdentityProviderCatalog next = TestIdentityProviders.small().withProviderAdded(google("google", "Google"));

        assertThat(next.providers()).extracting(IdentityProvider::id).containsExactly("keycloak", "github", "google");
        assertThatThrownBy(() -> next.withProviderAdded(google("google", "Otro")))
                .isInstanceOf(DuplicateIdentityProviderException.class);
    }

    @Test
    void aReplacedProviderKeepsItsPositionAndAnUnknownOneFails() {
        IdentityProviderCatalog catalog = TestIdentityProviders.small();
        IdentityProvider renamed = new IdentityProvider("github", "GitHub.com", "", ProviderHosting.CLOUD, null,
                List.of(), false, true, null);

        assertThat(catalog.withProviderReplaced(renamed).providers().get(1).name()).isEqualTo("GitHub.com");
        assertThatThrownBy(() -> catalog.withProviderReplaced(google("google", "Google")))
                .isInstanceOf(IdentityProviderNotFoundException.class);
    }
}