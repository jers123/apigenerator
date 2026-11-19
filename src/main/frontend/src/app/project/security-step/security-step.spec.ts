import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { SecurityDependencies, SecurityStep } from './security-step';
import { projectForm, toSecurityRequest } from "../project-form";
import { DependencyDefinition } from "../../catalog/catalog.models";
import { IdentityProviderDefinition, SecurityMechanismDefinition, SecurityRole } from "../../security/security.models";

describe('SecurityStep', () => {
  let http: HttpTestingController;

  const library = (artifactId: string): DependencyDefinition => ({
    name: artifactId, groupId: 'org.springframework.boot', artifactId, version: null, kind: 'LIBRARY',
    mavenScope: 'COMPILE', gradleScope: 'IMPLEMENTATION', includedByDefault: true, reactive: false,
  });
  const mechanism = (id: string, name: string, role: SecurityRole,
                     extra: Partial<SecurityMechanismDefinition> = {}): SecurityMechanismDefinition => ({
    id, name, description: `Descripción de ${name}`, role, reactiveSupported: true, requiresIssuer: false,
    managesUsers: false, conflictsWith: [], dependencies: [library('spring-boot-starter-security')], ...extra,
  });

  const mechanisms: SecurityMechanismDefinition[] = [
    mechanism('http-basic', 'HTTP Basic', 'API_AUTHENTICATION'),
    mechanism('oauth2-resource-server', 'OAuth2 Resource Server', 'API_AUTHENTICATION', {
      requiresIssuer: true, dependencies: [library('spring-boot-starter-security-oauth2-resource-server')],
    }),
    mechanism('form-login', 'Formulario', 'BROWSER_LOGIN', { conflictsWith: ['http-basic'], reactiveSupported: false }),
    mechanism('cors', 'CORS', 'ADD_ON'),
    mechanism('authorization-server', 'Servidor propio', 'AUTHORIZATION_SERVER', { reactiveSupported: false }),
  ];
  const keycloak: IdentityProviderDefinition = {
    id: 'keycloak', name: 'Keycloak', description: 'Código abierto', hosting: 'SELF_HOSTED',
    issuerUriTemplate: '{baseUrl}/realms/{realm}',
    parameters: [
      { key: 'baseUrl', label: 'URL de Keycloak', example: 'https://kc.example.com' },
      { key: 'realm', label: 'Realm', example: 'myrealm' },
    ],
    supportsResourceServer: true, supportsLogin: true, documentationUrl: null,
  };
  const github: IdentityProviderDefinition = {
    id: 'github', name: 'GitHub', description: 'OAuth2 sin OIDC', hosting: 'CLOUD', issuerUriTemplate: null,
    parameters: [], supportsResourceServer: false, supportsLogin: true, documentationUrl: null,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function settle(): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve));
    TestBed.tick();
  }

  async function render(reactive = false) {
    const form = projectForm().controls.security;
    const fixture = TestBed.createComponent(SecurityStep);
    fixture.componentRef.setInput('form', form);
    fixture.componentRef.setInput('reactive', reactive);
    fixture.componentRef.setInput('artifactId', 'api-clientes');
    TestBed.tick();
    http.expectOne('/api/v1/security/mechanisms').flush(mechanisms);
    http.expectOne('/api/v1/security/identity-providers').flush([keycloak, github]);
    await settle();
    await settle();
    return { fixture, form };
  }

  const page = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;
  const input = (fixture: { nativeElement: unknown }, testId: string) =>
    page(fixture).querySelector<HTMLInputElement>(`[data-testid="${testId}"] input`)!;
  const text = (fixture: { nativeElement: unknown }, testId: string) =>
    page(fixture).querySelector(`[data-testid="${testId}"]`)?.textContent ?? '';

  async function pick(fixture: { nativeElement: unknown }, testId: string) {
    input(fixture, testId).click();
    await settle();
  }

  it('por defecto la API es pública, sin login ni emisor', async () => {
    const { fixture, form } = await render();

    expect(input(fixture, 'api-none').checked).toBe(true);
    expect(input(fixture, 'login-none').checked).toBe(true);
    expect(page(fixture).querySelector('[data-testid="issuer-section"]')).toBeNull();
    expect(form.valid).toBe(true);
    expect(toSecurityRequest(form)).toEqual({ apiAuthenticationId: null, browserLoginId: null, addOnIds: [], issuer: null });
  });

  it('los complementos necesitan autenticación de la API', async () => {
    const { fixture, form } = await render();
    expect(input(fixture, 'addon-cors').disabled).toBe(true);
    expect(text(fixture, 'addon-cors')).toContain('Necesita autenticación de la API');

    await pick(fixture, 'api-http-basic');
    await pick(fixture, 'addon-cors');

    expect(toSecurityRequest(form)).toMatchObject({ apiAuthenticationId: 'http-basic', addOnIds: ['cors'] });
  });

  it('un login que choca con la autenticación elegida queda deshabilitado con el motivo', async () => {
    const { fixture } = await render();

    await pick(fixture, 'api-http-basic');

    expect(input(fixture, 'login-form-login').disabled).toBe(true);
    expect(text(fixture, 'login-form-login')).toContain('No se combina con HTTP Basic');
  });

  it('un método con emisor exige elegirlo; el servidor propio anuncia el segundo proyecto', async () => {
    const { fixture, form } = await render();
    let last: SecurityDependencies | undefined;
    fixture.componentInstance.dependenciesChange.subscribe((value) => (last = value));

    await pick(fixture, 'api-oauth2-resource-server');
    expect(text(fixture, 'issuer-section')).toContain('OAuth2 Resource Server');
    expect(form.valid).toBe(false);

    await pick(fixture, 'issuer-own');

    expect(form.valid).toBe(true);
    expect(text(fixture, 'issuer-own')).toContain('api-clientes-auth-server');
    expect(toSecurityRequest(form).issuer).toEqual({ type: 'OWN_SERVER', providerId: null, values: {} });
    expect(last?.chosen.map((dependency) => dependency.artifactId))
      .toEqual(['spring-boot-starter-security-oauth2-resource-server']);
    expect(last?.artifacts).toContain('org.springframework.boot:spring-boot-starter-security');
  });

  it('en reactivo bloquea el servidor propio y los métodos que no funcionan', async () => {
    const { fixture } = await render(true);

    await pick(fixture, 'api-oauth2-resource-server');

    expect(input(fixture, 'issuer-own').disabled).toBe(true);
    expect(input(fixture, 'login-form-login').disabled).toBe(true);
    expect(text(fixture, 'login-form-login')).toContain('reactivos');
  });

  it('con un proveedor pide sus datos y arma la URL del emisor', async () => {
    const { fixture, form } = await render();
    await pick(fixture, 'api-oauth2-resource-server');
    await pick(fixture, 'issuer-provider');

    document.querySelector<HTMLElement>('[data-testid="issuer-provider-select"] .mat-mdc-select-trigger')!.click();
    await settle();
    const options = [...document.querySelectorAll<HTMLElement>('mat-option')];
    expect(options.find((o) => o.textContent?.includes('GitHub'))?.getAttribute('aria-disabled')).toBe('true');
    options.find((o) => o.textContent?.includes('Keycloak'))!.click();
    await settle();
    expect(form.valid).toBe(false);

    for (const [key, value] of [['baseUrl', 'https://kc.example.com'], ['realm', 'ventas']]) {
      const field = page(fixture).querySelector<HTMLInputElement>(`[data-testid="issuer-value-${key}"]`)!;
      field.value = value;
      field.dispatchEvent(new Event('input'));
    }
    await settle();

    expect(text(fixture, 'issuer-uri')).toContain('https://kc.example.com/realms/ventas');
    expect(form.valid).toBe(true);
    expect(toSecurityRequest(form).issuer).toEqual({
      type: 'PROVIDER', providerId: 'keycloak', values: { baseUrl: 'https://kc.example.com', realm: 'ventas' },
    });
  });

  it('al pasar a reactivo quita lo que no funciona y avisa', async () => {
    const { fixture, form } = await render();
    await pick(fixture, 'login-form-login');
    expect(form.controls.browserLoginId.value).toBe('form-login');

    fixture.componentRef.setInput('reactive', true);
    await settle();

    expect(form.controls.browserLoginId.value).toBeNull();
    expect(text(fixture, 'security-notice')).toContain('Formulario');
  });
});