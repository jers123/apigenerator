import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EngineAdmin } from './engine-admin';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { DatabaseEngine } from '../database.models';
import { GroupDefinition } from '../../catalog/catalog.models';

describe('EngineAdmin', () => {
  let http: HttpTestingController;

  const postgresql: DatabaseEngine = {
    id: 'postgresql', label: 'PostgreSQL', driverGroupId: 'postgresql-driver', embedded: false, defaultPort: 5432,
    defaultUsername: 'postgres', jdbcUrlTemplate: 'jdbc:postgresql://{host}:{port}/{database}',
    kind: 'RELATIONAL', supportsSequences: true, exampleUrl: 'jdbc:postgresql://localhost:5432/mi_base',
  };
  const driver: GroupDefinition = {
    id: 'postgresql-driver', name: 'PostgreSQL Driver', category: 'DATABASE', selectedByDefault: false, requiresDatabase: false,
    dependencies: [{ name: 'PostgreSQL JDBC', groupId: 'org.postgresql', artifactId: 'postgresql', version: null,
      kind: 'LIBRARY', mavenScope: 'RUNTIME', gradleScope: 'RUNTIME_ONLY', includedByDefault: true, reactive: false }],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  /** Deja terminar las microtareas de los resource y ejecuta effects y detección de cambios. */
  async function settle(times = 3): Promise<void> {
    for (let i = 0; i < times; i++) {
      await new Promise((resolve) => setTimeout(resolve));
      TestBed.tick();
    }
  }

  const page = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;
  const click = (fixture: { nativeElement: unknown }, testId: string) =>
    page(fixture).querySelector<HTMLElement>(`[data-testid="${testId}"]`)!.click();

  /** Responde todas las consultas de versiones que haya hecho el editor del driver. */
  function flushVersions(): void {
    for (const call of http.match('/api/v1/versions')) {
      call.flush({ artifacts: [] });
    }
  }

  async function render() {
    const fixture = TestBed.createComponent(EngineAdmin);
    TestBed.tick();
    http.expectOne('/api/v1/databases').flush([postgresql]);
    await settle();
    return fixture;
  }

  async function openPostgresql(fixture: { nativeElement: unknown }) {
    click(fixture, 'edit-postgresql');
    await settle();
    http.expectOne('/api/v1/catalog/groups/postgresql-driver').flush(driver);
    await settle();
    flushVersions();
    await settle();
  }

  it('lista los motores con su URL de ejemplo', async () => {
    const fixture = await render();

    expect(page(fixture).querySelector('[data-testid="edit-postgresql"]')?.textContent)
      .toContain('jdbc:postgresql://localhost:5432/mi_base');
  });

  it('edita un motor y lo guarda con PUT junto con su driver, tipo y secuencias; después recarga la lista', async () => {
    const fixture = await render();
    await openPostgresql(fixture);

    click(fixture, 'save');
    await settle(1);

    const call = http.expectOne('/api/v1/databases/postgresql');
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual({
      id: 'postgresql', label: 'PostgreSQL', embedded: false, defaultPort: 5432, defaultUsername: 'postgres',
      jdbcUrlTemplate: 'jdbc:postgresql://{host}:{port}/{database}', driverGroup: driver,
      kind: 'RELATIONAL', supportsSequences: true,
    });
    call.flush(postgresql);
    await settle();
    http.expectOne('/api/v1/databases').flush([postgresql]);
    await settle();

    expect(page(fixture).querySelector('[data-testid="notice"]')?.textContent).toContain('guardado');
  });

  it('muestra la casilla de secuencias marcada según el motor', async () => {
    const fixture = await render();
    await openPostgresql(fixture);

    expect(page(fixture).querySelector<HTMLInputElement>('[data-testid="engine-sequences"] input')!.checked).toBe(true);
  });

  it('ofrece el tipo de documentos deshabilitado', async () => {
    const fixture = await render();
    click(fixture, 'new-engine');
    await settle();

    document.querySelector<HTMLElement>('[data-testid="engine-kind"] .mat-mdc-select-trigger')!.click();
    await settle();
    const options = [...document.querySelectorAll<HTMLElement>('mat-option')];

    expect(options.map((o) => o.textContent?.trim())).toEqual(['Relacional (SQL)', 'Documentos (NoSQL) (próximamente)']);
    expect(options[1].getAttribute('aria-disabled')).toBe('true');
  });

  it('el id del motor y el de su driver quedan fijos al editar, y la categoría es DATABASE', async () => {
    const fixture = await render();
    await openPostgresql(fixture);

    expect(page(fixture).querySelector<HTMLInputElement>('[data-testid="engine-id"]')!.readOnly).toBe(true);
    expect(page(fixture).querySelector<HTMLInputElement>('[data-testid="group-id"]')!.readOnly).toBe(true);
    expect(page(fixture).querySelector('[data-testid="group-category-fixed"]')?.textContent).toContain('Database');
  });

  it('muestra el detalle cuando el servidor rechaza el cambio', async () => {
    const fixture = await render();
    await openPostgresql(fixture);

    click(fixture, 'save');
    await settle(1);
    http.expectOne('/api/v1/databases/postgresql').flush(
      { title: 'Datos inválidos', status: 400, detail: "El grupo 'postgresql-driver' ya es el driver del motor 'otro'" },
      { status: 400, statusText: 'Bad Request' },
    );
    await settle();

    expect(page(fixture).querySelector('[data-testid="save-error"]')?.textContent).toContain('ya es el driver');
  });

  it('no envía un motor nuevo incompleto', async () => {
    const fixture = await render();
    click(fixture, 'new-engine');
    await settle();

    click(fixture, 'save');
    await settle();

    http.expectNone('/api/v1/databases');
    expect(page(fixture).querySelector('[data-testid="save-error"]')?.textContent).toContain('Revisa');
  });

  it('un motor embebido no pide puerto', async () => {
    const fixture = await render();
    click(fixture, 'new-engine');
    await settle();
    expect(page(fixture).querySelector('[data-testid="engine-port"]')).not.toBeNull();

    page(fixture).querySelector<HTMLInputElement>('[data-testid="engine-embedded"] input')!.click();
    await settle();

    expect(page(fixture).querySelector('[data-testid="engine-port"]')).toBeNull();
  });
});