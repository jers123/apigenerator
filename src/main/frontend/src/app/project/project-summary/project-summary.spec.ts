import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { ProjectSummary } from './project-summary';
import { ProjectPlan, ProjectRequest } from "../project.models";
import { EMPTY_DOMAIN_MODEL } from "../../model/model.models";
import { NO_CONSTRAINTS } from "../../model/model-form";

describe('ProjectSummary', () => {
  let http: HttpTestingController;

  const request: ProjectRequest = {
    metadata: { groupId: 'org.jers', artifactId: 'api', name: 'API', description: '' },
    build: { buildTool: 'GRADLE', javaVersion: 21, packaging: 'JAR', reactive: true, serverPort: 9090 },
    architecture: { style: 'HEXAGONAL', layout: 'SINGLE_MODULE', patterns: ['CQRS'] },
    options: { javadoc: true, unitTests: true, configFormat: 'PROPERTIES' },
    dependencyGroupIds: ['spring-web', 'springdoc', 'lombok'],
    database: { engineId: 'postgresql', host: 'localhost', port: 5432, databaseName: 'app', username: 'postgres', password: 'x' },
    testDatabase: null,
    versions: {},
    security: {
      apiAuthenticationId: 'oauth2-resource-server', browserLoginId: null, addOnIds: ['cors'],
      issuer: { type: 'OWN_SERVER', providerId: null, values: {} },
    },
    model: {
      ...EMPTY_DOMAIN_MODEL,
      audit: { createdAt: true, updatedAt: false },
      entities: [{
        id: 'e-1', name: 'Cliente', tableName: null, resourcePath: null, sequenceName: null, superclassId: null,
        attributes: [{
          id: 'a-1', name: 'nombre', columnName: null, type: { scalar: 'STRING', enumId: null },
          required: true, unique: false, constraints: NO_CONSTRAINTS,
        }],
        relations: [],
      }],
    },
  };

  const plan: ProjectPlan = {
    groupId: 'org.jers',
    artifactId: 'api',
    basePackage: 'org.jers.api',
    springBootVersion: '4.1.2',
    architecture: { style: 'HEXAGONAL', layout: 'SINGLE_MODULE', patterns: ['ADAPTER', 'CQRS'] },
    options: { javadoc: true, unitTests: true, configFormat: 'PROPERTIES' },
    persistence: 'DATABASE',
    testPersistence: 'IN_MEMORY',
    dependencies: [
      { name: 'Lombok', groupId: 'org.projectlombok', artifactId: 'lombok', kind: 'LIBRARY', mavenScope: 'PROVIDED',
        gradleScope: 'COMPILE_ONLY', version: null, managed: true },
      { name: 'Swagger UI', groupId: 'org.springdoc', artifactId: 'springdoc-openapi-starter-webmvc-ui', kind: 'LIBRARY',
        mavenScope: 'COMPILE', gradleScope: 'IMPLEMENTATION', version: '3.2.0', managed: false },
    ],
    database: { engineId: 'postgresql', jdbcUrl: 'jdbc:postgresql://localhost:5432/app', username: 'postgres' },
    testDatabase: null,
    security: {
      apiAuthentication: 'OAuth2 Resource Server', browserLogin: null, addOns: ['CORS'], issuerType: 'OWN_SERVER',
      providerName: null, issuerUri: 'http://localhost:9000', managesUsers: false,
      authorizationServerArtifactId: 'api-auth-server', authorizationServerPort: 9000,
    },
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

  async function render(sent: ProjectRequest | null) {
    const fixture = TestBed.createComponent(ProjectSummary);
    fixture.componentRef.setInput('request', sent);
    TestBed.tick();
    await settle();
    return fixture;
  }

  const page = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;
  const text = (fixture: { nativeElement: unknown }, testId: string) =>
    page(fixture).querySelector(`[data-testid="${testId}"]`)?.textContent ?? '';

  it('antes de llegar al paso no pide el plan', async () => {
    const fixture = await render(null);

    http.expectNone('/api/v1/projects/plan');
    expect(text(fixture, 'summary-empty')).toContain('al llegar a este paso');
  });

  it('envía la petición tal cual y muestra el plan por secciones', async () => {
    const fixture = await render(request);

    const call = http.expectOne('/api/v1/projects/plan');
    expect(call.request.body).toEqual(request);
    call.flush(plan);
    await settle();

    expect(text(fixture, 'summary-project')).toContain('org.jers.api');
    expect(text(fixture, 'summary-project')).toContain('4.1.2');
    expect(text(fixture, 'summary-project')).toContain('Gradle · Java 21');
    expect(text(fixture, 'summary-architecture')).toContain('ADAPTER');
    expect(text(fixture, 'summary-data')).toContain('Base de datos');
    expect(text(fixture, 'summary-data')).toContain('En memoria (RAM)');
    expect(text(fixture, 'summary-data')).toContain('jdbc:postgresql://localhost:5432/app');
    expect(text(fixture, 'summary-options')).toContain('application.properties');
    expect(text(fixture, 'summary-dependencies')).toContain('gestionada');
    expect(text(fixture, 'summary-dependencies')).toContain('3.2.0');
    expect(text(fixture, 'summary-security')).toContain('OAuth2 Resource Server');
    expect(text(fixture, 'summary-security')).toContain('http://localhost:9000');
    expect(text(fixture, 'summary-auth-server')).toContain('api-auth-server');
    expect(page(fixture).querySelector<HTMLButtonElement>('[data-testid="generate"]')!.disabled).toBe(true);
  });

  it('resume el modelo: entidades, atributos, id y auditoría', async () => {
    const fixture = await render(request);
    http.expectOne('/api/v1/projects/plan').flush(plan);
    await settle();

    const model = text(fixture, 'summary-model');
    expect(model).toContain('1 · 1 atributos · 0 relaciones');
    expect(model).toContain('Long · Autoincremento (IDENTITY)');
    expect(model).toContain('En cada entidad');
    expect(model).toContain('createdAt');
  });

  it('sin entidades indica que el proyecto no tendrá CRUD', async () => {
    const fixture = await render({ ...request, model: EMPTY_DOMAIN_MODEL });
    http.expectOne('/api/v1/projects/plan').flush(plan);
    await settle();

    expect(text(fixture, 'summary-model')).toContain('sin CRUD');
  });

  it('muestra el motivo cuando el servidor rechaza el proyecto', async () => {
    const fixture = await render(request);

    http.expectOne('/api/v1/projects/plan').flush(
      { title: 'Datos inválidos', status: 400, detail: 'Estos grupos necesitan una base de datos de desarrollo: [Spring Data]' },
      { status: 400, statusText: 'Bad Request' },
    );
    await settle();

    expect(text(fixture, 'summary-error')).toContain('Spring Data');
  });

  it('vuelve a pedir el plan cuando llega una petición nueva', async () => {
    const fixture = await render(request);
    http.expectOne('/api/v1/projects/plan').flush(plan);
    await settle();

    fixture.componentRef.setInput('request', { ...request, versions: { 'org.projectlombok:lombok': '1.18.42' } });
    await settle();

    const call = http.expectOne('/api/v1/projects/plan');
    expect(call.request.body.versions).toEqual({ 'org.projectlombok:lombok': '1.18.42' });
    call.flush(plan);
    await settle();
  });
});