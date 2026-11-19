package org.jers.apigenerator.web.project;

import org.jers.apigenerator.core.project.ProjectPlan;
import org.jers.apigenerator.core.security.ResolvedSecurity;
import org.jers.apigenerator.core.security.SecurityMechanism;
import org.jers.apigenerator.core.security.SecurityResolver;

import java.util.List;

/**
 * Seguridad del plan, para el resumen del asistente.
 *
 * @param apiAuthentication             nombre de la autenticación de la API, o {@code null}
 * @param browserLogin                  nombre del inicio de sesión, o {@code null}
 * @param addOns                        nombres de los complementos
 * @param issuerType                    tipo de emisor, o {@code null} si no hay
 * @param providerName                  nombre del proveedor, o {@code null}
 * @param issuerUri                     URL del emisor, o {@code null}
 * @param managesUsers                  si el proyecto guarda usuarios
 * @param authorizationServerArtifactId artifactId del segundo proyecto, o {@code null}
 * @param authorizationServerPort       puerto del segundo proyecto, o {@code null}
 */
public record SecurityPlanResponse(
        String apiAuthentication,
        String browserLogin,
        List<String> addOns,
        IssuerRequest.Type issuerType,
        String providerName,
        String issuerUri,
        boolean managesUsers,
        String authorizationServerArtifactId,
        Integer authorizationServerPort) {

    /**
     * Convierte la seguridad de un plan.
     *
     * @param plan plan del proyecto
     * @return representación para la API
     */
    public static SecurityPlanResponse from(final ProjectPlan plan) {
        ResolvedSecurity security = plan.security();
        boolean ownServer = security.generatesAuthorizationServer();
        return new SecurityPlanResponse(
                nameOf(security.apiAuthentication()),
                nameOf(security.browserLogin()),
                security.addOns().stream().map(SecurityMechanism::name).toList(),
                IssuerRequest.of(ownServer, security.provider() != null),
                security.provider() == null ? null : security.provider().name(),
                security.issuerUri(),
                security.managesUsers(),
                plan.authorizationServerArtifactId(),
                ownServer ? SecurityResolver.AUTHORIZATION_SERVER_PORT : null);
    }

    /**
     * Nombre de un método.
     *
     * @param mechanism método, o {@code null}
     * @return su nombre, o {@code null}
     */
    private static String nameOf(final SecurityMechanism mechanism) {
        return mechanism == null ? null : mechanism.name();
    }
}