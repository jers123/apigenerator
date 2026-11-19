import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { firstValueFrom } from "rxjs";
import { SecurityApi } from "./security-api";
import { IdentityProviderDefinition, SecurityMechanismDefinition } from "./security.models";

describe('SecurityApi', () => {
  let api: SecurityApi;
  let http: HttpTestingController;

  const cors: SecurityMechanismDefinition = {
    id: 'cors', name: 'CORS', description: '', role: 'ADD_ON', reactiveSupported: true, requiresIssuer: false,
    managesUsers: false, conflictsWith: [],
    dependencies: [{ name: 'Spring Security', groupId: 'org.springframework.boot', artifactId: 'spring-boot-starter-security',
      version: null, kind: 'LIBRARY', mavenScope: 'COMPILE', gradleScope: 'IMPLEMENTATION', includedByDefault: true, reactive: false }],
  };
  const auth0: IdentityProviderDefinition = {
    id: 'auth0', name: 'Auth0', description: '', hosting: 'CLOUD', issuerUriTemplate: 'https://{domain}/',
    parameters: [{ key: 'domain', label: 'Dominio', example: 't.us.auth0.com' }],
    supportsResourceServer: true, supportsLogin: true, documentationUrl: null,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(SecurityApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista los métodos con GET', async () => {
    const result = firstValueFrom(api.mechanisms());
    http.expectOne({ method: 'GET', url: '/api/v1/security/mechanisms' }).flush([cors]);
    expect(await result).toEqual([cors]);
  });

  it('crea y edita métodos con POST y PUT a su id', async () => {
    const created = firstValueFrom(api.createMechanism(cors));
    const post = http.expectOne({ method: 'POST', url: '/api/v1/security/mechanisms' });
    expect(post.request.body).toEqual(cors);
    post.flush(cors, { status: 201, statusText: 'Created' });
    expect(await created).toEqual(cors);

    const updated = firstValueFrom(api.updateMechanism(cors));
    http.expectOne({ method: 'PUT', url: '/api/v1/security/mechanisms/cors' }).flush(cors);
    expect(await updated).toEqual(cors);
  });

  it('lista los proveedores con GET', async () => {
    const result = firstValueFrom(api.providers());
    http.expectOne({ method: 'GET', url: '/api/v1/security/identity-providers' }).flush([auth0]);
    expect(await result).toEqual([auth0]);
  });

  it('crea y edita proveedores con POST y PUT a su id', async () => {
    const created = firstValueFrom(api.createProvider(auth0));
    http.expectOne({ method: 'POST', url: '/api/v1/security/identity-providers' })
      .flush(auth0, { status: 201, statusText: 'Created' });
    expect(await created).toEqual(auth0);

    const updated = firstValueFrom(api.updateProvider(auth0));
    http.expectOne({ method: 'PUT', url: '/api/v1/security/identity-providers/auth0' }).flush(auth0);
    expect(await updated).toEqual(auth0);
  });
});