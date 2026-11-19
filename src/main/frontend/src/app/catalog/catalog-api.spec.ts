import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { firstValueFrom } from "rxjs";
import { CatalogApi } from './catalog-api';
import { Catalog, Dependency, GroupDefinition, ResolveRequest, ResolveResponse } from './catalog.models';
import { ProblemDetail } from '../shared/problem-detail';

describe('CatalogApi', () => {
  let api: CatalogApi;
  let http: HttpTestingController;

  const webMvc: Dependency = {
    name: 'Spring Web MVC',
    groupId: 'org.springframework.boot',
    artifactId: 'spring-boot-starter-webmvc',
    version: null,
    kind: 'LIBRARY',
    mavenScope: 'COMPILE',
    gradleScope: 'IMPLEMENTATION',
    includedByDefault: true,
    reactive: false,
    followsSpringBoot: true,
  };

  const catalog: Catalog = {
    groups: [
      {
        id: 'spring-web',
        name: 'Spring Web',
        category: 'SPRING',
        categoryLabel: 'Spring',
        selectedByDefault: true,
        requiresDatabase: false,
        standardDependencies: [webMvc],
        reactiveDependencies: [],
      },
    ],
  };

  const request: ResolveRequest = {groupIds: ['spring-web'], buildTool: 'MAVEN', reactive: false };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(CatalogApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide el catálogo con GET /api/v1/catalog', async () => {
    const result = firstValueFrom(api.getCatalog());

    const call = http.expectOne('/api/v1/catalog');
    expect(call.request.method).toBe('GET');
    call.flush(catalog);

    expect(await result).toEqual(catalog);
  });

  it('envía la selección con POST /api/v1/catalog/resolve sin alterarla', async () => {
    const response: ResolveResponse = { groupIds: ['maven', 'spring-web'], dependencies: [] };
    const result = firstValueFrom(api.resolve(request));

    const call = http.expectOne('/api/v1/catalog/resolve');
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(request);
    call.flush(response);

    expect(await result).toEqual(response);
  });

  it('entrega el ProblemDetail cuando la API responde 400', async () => {
    const problem: ProblemDetail = {
      title: 'Grupo de dependencias inexistente',
      status: 400,
      detail: 'Grupos inexistentes en el catálogo: [kafka]',
      groupIds: ['kafka'],
    };
    const result = firstValueFrom(api.resolve({ ...request, groupIds: ['kafka'] }));

    http.expectOne('/api/v1/catalog/resolve').flush(problem, { status: 400, statusText: 'Bad Request' });

    await expect(result).rejects.toMatchObject({ status: 400, error: { groupIds: ['kafka'] } });
  });

  const lombokGroup: GroupDefinition = {
    id: 'lombok',
    name: 'Lombok',
    category: 'OTHER',
    selectedByDefault: false,
    requiresDatabase: false,
    dependencies: [{
      name: 'Lombok', groupId: 'org.projectlombok', artifactId: 'lombok', version: null, kind: 'LIBRARY',
      mavenScope: 'PROVIDED', gradleScope: 'COMPILE_ONLY', includedByDefault: true, reactive: false,
    }],
  };

  it('pide la definición de un grupo con GET /api/v1/catalog/groups/{id}', async () => {
    const result = firstValueFrom(api.getGroup('lombok'));

    const call = http.expectOne('/api/v1/catalog/groups/lombok');
    expect(call.request.method).toBe('GET');
    call.flush(lombokGroup);

    expect(await result).toEqual(lombokGroup);
  });

  it('crea un grupo con POST /api/v1/catalog/groups', async () => {
    const result = firstValueFrom(api.createGroup(lombokGroup));

    const call = http.expectOne('/api/v1/catalog/groups');
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(lombokGroup);
    call.flush(lombokGroup, { status: 201, statusText: 'Created' });

    expect(await result).toEqual(lombokGroup);
  });

  it('edita un grupo con PUT a la ruta de su id', async () => {
    const result = firstValueFrom(api.updateGroup(lombokGroup));

    const call = http.expectOne('/api/v1/catalog/groups/lombok');
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual(lombokGroup);
    call.flush(lombokGroup);

    expect(await result).toEqual(lombokGroup);
  });
});