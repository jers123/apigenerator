package org.jers.apigenerator.core.security;

import org.jers.apigenerator.testsupport.TestIdentityProviders;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class IdentityProviderTest {

    private static IdentityProvider provider(final String id, final String template,
                                             final List<ProviderParameter> parameters,
                                             final boolean resourceServer, final boolean login) {
        return new IdentityProvider(id, "Proveedor " + id, null, ProviderHosting.CLOUD, template, parameters,
                resourceServer, login, null);
    }

    private static ProviderParameter parameter(final String key) {
        return new ProviderParameter(key, "Dato " + key, null);
    }

    @Test
    void theIssuerIsBuiltFromTheTemplateAndTheValues() {
        String issuer = TestIdentityProviders.keycloak()
                .issuerUri(Map.of("baseUrl", " https://kc.example.com ", "realm", "ventas"));

        assertThat(issuer).isEqualTo("https://kc.example.com/realms/ventas");
    }

    @Test
    void missingValuesAreNamedByTheirLabel() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> TestIdentityProviders.keycloak().issuerUri(Map.of("baseUrl", "https://kc.example.com")))
                .withMessageContaining("Realm");
    }

    @Test
    void aProviderWithoutTemplateHasNoIssuer() {
        assertThat(TestIdentityProviders.github().issuerUri(Map.of())).isNull();
    }

    @Test
    void everyPlaceholderNeedsItsParameter() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider("entra-id", "https://login.microsoftonline.com/{tenantId}/v2.0", List.of(),
                        true, true))
                .withMessageContaining("tenantId");
    }

    @Test
    void unusedParametersAreRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider("auth0", "https://{domain}/", List.of(parameter("domain"), parameter("region")),
                        true, true))
                .withMessageContaining("region");
    }

    @Test
    void theTemplateMustBeHttpsOrStartWithAParameter() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider("inseguro", "http://idp.local/", List.of(), true, true))
                .withMessageContaining("https://");
    }

    @Test
    void aProviderForTheApiNeedsAnIssuerTemplate() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider("github", null, List.of(), true, true))
                .withMessageContaining("validar sus tokens");
    }

    @Test
    void onlyProvidersKnownBySpringMayOmitTheTemplate() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider("mi-idp", null, List.of(), false, true))
                .withMessageContaining("github");
    }

    @Test
    void aProviderMustServeTheApiOrTheLogin() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider("nada", "https://idp.example.com", List.of(), false, false))
                .withMessageContaining("ambos");
    }

    @Test
    void parameterKeysMustBeIdentifiers() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> parameter("tenant-id"))
                .withMessageContaining("key");
    }
}