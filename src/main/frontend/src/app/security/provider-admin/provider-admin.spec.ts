import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { ProviderAdmin } from "./provider-admin";
import { IdentityProviderDefinition } from "../security.models";

describe('ProviderAdmin', () => {
  let http: HttpTestingController;

  const keycloak: IdentityProviderDefinition = {
    id: 'keycloak', name: 'Keycloak', description: 'Código abierto', hosting: 'SELF_HOSTED',
    issuerUriTemplate: '{baseUrl}/realms/{realm}',
    parameters: [
      { key: 'baseUrl', label: 'URL de Keycloak', example: 'https://kc.example.com' },
      { key: 'realm', label: 'Realm', example: 'myrealm' },
    ],
    supportsResourceServer: true, supportsLogin: true, documentationUrl: 'https://www.keycloak.org/documentation',
  };
  const github: IdentityProviderDefinition = {
    id: 'github', name: 'GitHub', description: 'OAuth2 sin OIDC', hosting: 'CLOUD', issuerUriTemplate: null,
    parameters: [], supportsResourceServer: false, supportsLogin: true, documentationUrl: null,
  };
  const url = '/api/v1/security/identity-providers';

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function settle(times = 3): Promise<void> {
    for (let i = 0; i < times; i++) {
      await new Promise((resolve) => setTimeout(resolve));
      TestBed.tick();
    }
  }

  const page = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;
  const find = <T extends HTMLElement>(fixture: { nativeElement: unknown }, testId: string) =>
    page(fixture).querySelector<T>(`[data-testid="${testId}"]`);
  const click = (fixture: { nativeElement: unknown }, testId: string) => find(fixture, testId)!.click();

  async function render() {
    const fixture = TestBed.createComponent(ProviderAdmin);
    TestBed.tick();
    http.expectOne(url).flush([keycloak, github]);
    await settle();
    return fixture;
  }

  it('lista los proveedores con su alojamiento y sus usos', async () => {
    const fixture = await render();

    expect(find(fixture, 'edit-keycloak')?.textContent).toContain('Lo instalas tú · API · Login');
    expect(find(fixture, 'edit-github')?.textContent).toContain('En la nube · Login');
  });

  it('muestra la URL del emisor armada con los ejemplos', async () => {
    const fixture = await render();
    click(fixture, 'edit-keycloak');
    await settle();

    expect(find(fixture, 'issuer-preview')?.textContent).toContain('https://kc.example.com/realms/myrealm');
  });

  it('edita un proveedor y lo guarda con PUT sin alterarlo', async () => {
    const fixture = await render();
    click(fixture, 'edit-keycloak');
    await settle();

    click(fixture, 'save');
    await settle(1);

    const call = http.expectOne(`${url}/keycloak`);
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual(keycloak);
    call.flush(keycloak);
    await settle();
    http.expectOne(url).flush([keycloak, github]);
    await settle();

    expect(find(fixture, 'notice')?.textContent).toContain('guardado');
  });

  it('crea los parámetros que pide la plantilla', async () => {
    const fixture = await render();
    click(fixture, 'new-provider');
    await settle();

    const template = find<HTMLInputElement>(fixture, 'provider-template')!;
    template.value = 'https://{domain}/';
    template.dispatchEvent(new Event('input'));
    await settle();
    expect(find(fixture, 'undeclared')?.textContent).toContain('domain');

    click(fixture, 'add-missing-parameters');
    await settle();

    expect(find<HTMLInputElement>(fixture, 'param-key-0')!.value).toBe('domain');
    expect(find(fixture, 'undeclared')).toBeNull();
  });

  it('no envía un proveedor nuevo incompleto', async () => {
    const fixture = await render();
    click(fixture, 'new-provider');
    await settle();

    click(fixture, 'save');
    await settle();

    http.expectNone(url);
    expect(find(fixture, 'save-error')?.textContent).toContain('Revisa');
  });
});