package org.jers.apigenerator.core.security;

import org.jers.apigenerator.core.catalog.Dependency;
import org.jers.apigenerator.testsupport.TestIdentityProviders;
import org.jers.apigenerator.testsupport.TestSecurityCatalogs;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SecurityResolverTest {

    private static final SecurityMechanism OIDC_LOGIN = TestSecurityCatalogs.mechanism("oidc-login", "OIDC Login",
            SecurityRole.BROWSER_LOGIN, true, Set.of());

    private static final Map<String, String> KEYCLOAK_VALUES =
            Map.of("baseUrl", "https://kc.example.com", "realm", "ventas");

    private final SecurityResolver resolver = new SecurityResolver(
            TestSecurityCatalogs.small().withMechanismAdded(OIDC_LOGIN), TestIdentityProviders.small());

    private static SecuritySelection api(final String apiId, final TokenIssuer issuer, final String... addOns) {
        return new SecuritySelection(apiId, null, Set.of(addOns), issuer);
    }

    private static TokenIssuer provider(final String id, final Map<String, String> values) {
        return new TokenIssuer.ExternalProvider(id, values);
    }

    @Test
    void withoutSecurityNothingIsAdded() {
        ResolvedSecurity security = resolver.resolve(SecuritySelection.none(), false);

        assertThat(security.enabled()).isFalse();
        assertThat(security.dependencies()).isEmpty();
        assertThat(security.generatesAuthorizationServer()).isFalse();
    }

    @Test
    void sharedArtifactsAppearOnce() {
        ResolvedSecurity security = resolver.resolve(api("http-basic", null, "cors"), false);

        assertThat(security.mechanisms()).extracting(SecurityMechanism::id).containsExactly("http-basic", "cors");
        assertThat(security.dependencies()).extracting(Dependency::artifactId)
                .containsExactly("spring-boot-starter-security");
    }

    @Test
    void addOnsNeedApiAuthentication() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolver.resolve(api(null, null, "cors"), false))
                .withMessageContaining("CORS");
    }

    @Test
    void conflictingMechanismsAreRejectedNamingBoth() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolver.resolve(new SecuritySelection("http-basic", "form-login", Set.of(), null),
                        false))
                .withMessageContaining("HTTP Basic")
                .withMessageContaining("Formulario");
    }

    @Test
    void aMechanismOfAnotherRoleIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolver.resolve(api("authorization-server", null), false))
                .withMessageContaining("no de tipo");
    }

    @Test
    void anUnknownMechanismIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolver.resolve(api("kerberos", null), false))
                .withMessageContaining("kerberos");
    }

    @Test
    void aMechanismThatNeedsAnIssuerRequiresOne() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolver.resolve(api("oauth2-resource-server", null), false))
                .withMessageContaining("emisor");
    }

    @Test
    void anIssuerThatNoMechanismUsesIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolver.resolve(api("http-basic", new TokenIssuer.OwnAuthorizationServer()), false))
                .withMessageContaining("Ningún método");
    }

    @Test
    void theOwnServerIsGeneratedAndIssuesFromPort9000() {
        ResolvedSecurity security = resolver.resolve(
                api("oauth2-resource-server", new TokenIssuer.OwnAuthorizationServer()), false);

        assertThat(security.generatesAuthorizationServer()).isTrue();
        assertThat(security.issuerUri()).isEqualTo("http://localhost:9000");
    }

    @Test
    void theOwnServerIsBlockedInReactiveProjects() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolver.resolve(
                        api("oauth2-resource-server", new TokenIssuer.OwnAuthorizationServer()), true))
                .withMessageContaining("reactivo");
    }

    @Test
    void aProviderBuildsItsIssuerFromTheValues() {
        ResolvedSecurity security = resolver.resolve(
                api("oauth2-resource-server", provider("keycloak", KEYCLOAK_VALUES)), false);

        assertThat(security.provider().id()).isEqualTo("keycloak");
        assertThat(security.issuerUri()).isEqualTo("https://kc.example.com/realms/ventas");
        assertThat(security.generatesAuthorizationServer()).isFalse();
    }

    @Test
    void aLoginOnlyProviderCannotIssueTokensForTheApi() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolver.resolve(api("oauth2-resource-server", provider("github", Map.of())), false))
                .withMessageContaining("API");
    }

    @Test
    void aLoginOnlyProviderServesTheLogin() {
        ResolvedSecurity security = resolver.resolve(
                new SecuritySelection(null, "oidc-login", Set.of(), provider("github", Map.of())), false);

        assertThat(security.provider().id()).isEqualTo("github");
        assertThat(security.issuerUri()).isNull();
    }

    @Test
    void valuesTheProviderDoesNotUseAreRejected() {
        Map<String, String> values = new HashMap<>(KEYCLOAK_VALUES);
        values.put("extra", "x");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolver.resolve(api("oauth2-resource-server", provider("keycloak", values)), false))
                .withMessageContaining("extra");
    }

    @Test
    void servletOnlyMechanismsAreRejectedInReactiveProjects() {
        SecurityMechanism saml = new SecurityMechanism("saml", "SAML", "", SecurityRole.API_AUTHENTICATION,
                TestSecurityCatalogs.small().findMechanism("http-basic").orElseThrow().dependencies(),
                false, false, false, Set.of());
        SecurityResolver withSaml = new SecurityResolver(TestSecurityCatalogs.small().withMechanismAdded(saml),
                TestIdentityProviders.small());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> withSaml.resolve(api("saml", null), true))
                .withMessageContaining("SAML");
    }
}