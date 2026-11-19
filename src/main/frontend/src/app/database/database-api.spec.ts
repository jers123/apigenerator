import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { firstValueFrom } from "rxjs";
import { DatabaseEngine, DatabaseEngineRequest } from './database.models';
import { DatabaseApi } from './database-api';

describe('DatabaseApi', () => {
  let api: DatabaseApi;
  let http: HttpTestingController;

  const request: DatabaseEngineRequest = {
    id: 'mariadb', label: 'MariaDB', embedded: false, defaultPort: 3306, defaultUsername: 'root',
    jdbcUrlTemplate: 'jdbc:mariadb://{host}:{port}/{database}',
    driverGroup: {
      id: 'mariadb-driver', name: 'MariaDB Driver', category: 'DATABASE', selectedByDefault: false, requiresDatabase: false,
      dependencies: [{ name: 'MariaDB JDBC', groupId: 'org.mariadb.jdbc', artifactId: 'mariadb-java-client',
        version: null, kind: 'LIBRARY', mavenScope: 'RUNTIME', gradleScope: 'RUNTIME_ONLY',
        includedByDefault: true, reactive: false }],
    },
    kind: 'RELATIONAL',
    supportsSequences: true,
  };
  const saved: DatabaseEngine = {
    id: 'mariadb', label: 'MariaDB', driverGroupId: 'mariadb-driver', embedded: false, defaultPort: 3306,
    defaultUsername: 'root', jdbcUrlTemplate: 'jdbc:mariadb://{host}:{port}/{database}',
    kind: 'RELATIONAL', supportsSequences: true, exampleUrl: 'jdbc:mariadb://localhost:3306/mi_base',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(DatabaseApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista los motores con GET /api/v1/databases', async () => {
    const result = firstValueFrom(api.list());

    const call = http.expectOne('/api/v1/databases');
    expect(call.request.method).toBe('GET');
    call.flush([saved]);

    expect(await result).toEqual([saved]);
  });

  it('crea un motor con POST sin alterar el cuerpo', async () => {
    const result = firstValueFrom(api.create(request));

    const call = http.expectOne('/api/v1/databases');
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(request);
    call.flush(saved, { status: 201, statusText: 'Created' });

    expect(await result).toEqual(saved);
  });

  it('edita un motor con PUT a la ruta de su id', async () => {
    const result = firstValueFrom(api.update(request));

    const call = http.expectOne('/api/v1/databases/mariadb');
    expect(call.request.method).toBe('PUT');
    call.flush(saved);

    expect(await result).toEqual(saved);
  });
});