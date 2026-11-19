import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { firstValueFrom } from "rxjs";
import { ProjectApi } from './project-api';
import { ArchitectureCatalog, ProjectRequest } from "./project.models";
import { EMPTY_DOMAIN_MODEL } from "../model/model.models";

describe('ProjectApi', () => {
  let api: ProjectApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(ProjectApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide el catálogo de arquitecturas con GET /api/v1/architectures', async () => {
    const catalog: ArchitectureCatalog = { styles: [], layouts: [], patterns: [] };
    const result = firstValueFrom(api.architectures());

    const call = http.expectOne('/api/v1/architectures');
    expect(call.request.method).toBe('GET');
    call.flush(catalog);

    expect(await result).toEqual(catalog);
  });

  it('pide el plan con POST /api/v1/projects/plan sin alterar el cuerpo', () => {
    const request: ProjectRequest = {
      metadata: { groupId: 'org.jers', artifactId: 'api', name: 'API', description: '' },
      build: { buildTool: 'MAVEN', javaVersion: 25, packaging: 'JAR', reactive: false, serverPort: 8080 },
      architecture: { style: 'LAYERED', layout: 'SINGLE_MODULE', patterns: [] },
      options: { javadoc: true, unitTests: true, configFormat: 'YAML' },
      dependencyGroupIds: ['spring-boot'],
      database: null,
      testDatabase: null,
      versions: {},
      security: { apiAuthenticationId: null, browserLoginId: null, addOnIds: [], issuer: null },
      model: EMPTY_DOMAIN_MODEL,
    };

    api.plan(request).subscribe();

    const call = http.expectOne('/api/v1/projects/plan');
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(request);
    call.flush({});
  });
});