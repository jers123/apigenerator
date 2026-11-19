import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { firstValueFrom } from "rxjs";
import { ModelApi } from "./model-api";
import { DomainModel, EMPTY_DOMAIN_MODEL, ModelDependencyGroups, ModelValidationResult } from "./model.models";

describe('ModelApi', () => {
  let api: ModelApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(ModelApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('valida con POST /api/v1/models/validate sin alterar el modelo', async () => {
    const model: DomainModel = {
      ...EMPTY_DOMAIN_MODEL,
      entities: [
        {
          id: 'e-1',
          name: 'Cliente',
          tableName: null,
          resourcePath: null,
          sequenceName: null,
          superclassId: null,
          attributes: [],
          relations: [],
        },
      ],
    };
    const response: ModelValidationResult = {
      valid: true,
      issues: [],
      entities: [],
      superclasses: [],
      relations: [],
    };
    const result = firstValueFrom(api.validate(model));

    const call = http.expectOne('/api/v1/models/validate');
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(model);
    call.flush(response);

    expect(await result).toEqual(response);
  });

  it('pide los grupos del modelo con GET /api/v1/models/dependency-groups', async () => {
    const groups: ModelDependencyGroups = { persistenceGroupId: 'spring-data', validationGroupId: 'validation' };
    const result = firstValueFrom(api.dependencyGroups());

    const call = http.expectOne('/api/v1/models/dependency-groups');
    expect(call.request.method).toBe('GET');
    call.flush(groups);

    expect(await result).toEqual(groups);
  });
});