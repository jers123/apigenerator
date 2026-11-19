import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { firstValueFrom } from "rxjs";
import { VersionApi } from "./version-api";
import { VersionLookupResponse } from "./version.models";

describe('VersionApi', () => {
  let api: VersionApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(VersionApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta todos los artefactos con un solo POST /api/v1/versions', async () => {
    const request = { artifacts: [{ groupId: 'com.h2database', artifactId: 'h2' }] };
    const response: VersionLookupResponse = {
      artifacts: [{ groupId: 'com.h2database', artifactId: 'h2', latest: '2.4.240', versions: ['2.4.240'], available: true }],
    };
    const result = firstValueFrom(api.lookup(request));

    const call = http.expectOne('/api/v1/versions');
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(request);
    call.flush(response);

    expect(await result).toEqual(response);
  });
});