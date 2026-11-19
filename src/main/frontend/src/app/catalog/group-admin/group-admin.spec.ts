import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { GroupAdmin } from './group-admin';
import { DependencyGroup, GroupDefinition } from "../catalog.models";

describe('GroupAdmin', () => {
  let http: HttpTestingController;

  const lombokSummary: DependencyGroup = {
    id: 'lombok', name: 'Lombok', category: 'OTHER', categoryLabel: 'Other', selectedByDefault: false, requiresDatabase: false,
    standardDependencies: [], reactiveDependencies: [],
  };
  const h2Summary: DependencyGroup = {
    ...lombokSummary, id: 'h2-database', name: 'H2 Database', category: 'DATABASE', categoryLabel: 'Database',
  };
  const lombok: GroupDefinition = {
    id: 'lombok', name: 'Lombok', category: 'OTHER', selectedByDefault: false, requiresDatabase: false,
    dependencies: [{ name: 'Lombok', groupId: 'org.projectlombok', artifactId: 'lombok', version: null,
      kind: 'LIBRARY', mavenScope: 'PROVIDED', gradleScope: 'COMPILE_ONLY', includedByDefault: true, reactive: false }],
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

  /** Responde todas las consultas de versiones que haya hecho el editor. */
  function flushVersions(): void {
    for (const call of http.match('/api/v1/versions')) {
      call.flush({ artifacts: [] });
    }
  }

  async function render() {
    const fixture = TestBed.createComponent(GroupAdmin);
    TestBed.tick();
    http.expectOne('/api/v1/catalog').flush({ groups: [lombokSummary, h2Summary] });
    await settle();
    return fixture;
  }

  async function openLombok(fixture: { nativeElement: unknown }) {
    click(fixture, 'edit-lombok');
    await settle();
    http.expectOne('/api/v1/catalog/groups/lombok').flush(lombok);
    await settle();
    flushVersions();
    await settle();
  }

  it('lista solo los grupos que no son de base de datos', async () => {
    const fixture = await render();

    expect(page(fixture).querySelector('[data-testid="edit-lombok"]')).not.toBeNull();
    expect(page(fixture).querySelector('[data-testid="edit-h2-database"]')).toBeNull();
  });

  it('edita un grupo, lo guarda con PUT y después recarga la lista', async () => {
    const fixture = await render();
    await openLombok(fixture);
    const name = page(fixture).querySelector<HTMLInputElement>('[data-testid="group-name"]')!;
    name.value = 'Project Lombok';
    name.dispatchEvent(new Event('input'));

    click(fixture, 'save');
    await settle(1);

    const call = http.expectOne('/api/v1/catalog/groups/lombok');
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual({ ...lombok, name: 'Project Lombok' });
    call.flush({ ...lombok, name: 'Project Lombok' });
    await settle();
    http.expectOne('/api/v1/catalog').flush({ groups: [{ ...lombokSummary, name: 'Project Lombok' }, h2Summary] });
    await settle();

    expect(page(fixture).querySelector('[data-testid="notice"]')?.textContent).toContain('Project Lombok');
    expect(page(fixture).querySelector('[data-testid="edit-lombok"]')?.textContent).toContain('Project Lombok');
  });

  it('no envía un grupo nuevo incompleto', async () => {
    const fixture = await render();
    click(fixture, 'new-group');
    await settle();

    click(fixture, 'save');
    await settle();

    http.expectNone('/api/v1/catalog/groups');
    expect(page(fixture).querySelector('[data-testid="save-error"]')?.textContent).toContain('Revisa');
  });

  it('muestra el detalle cuando el servidor rechaza el cambio', async () => {
    const fixture = await render();
    await openLombok(fixture);

    click(fixture, 'save');
    await settle(1);
    http.expectOne('/api/v1/catalog/groups/lombok').flush(
      { title: 'Datos inválidos', status: 400, detail: 'Nombre de grupo repetido en el catálogo: Spring Web' },
      { status: 400, statusText: 'Bad Request' },
    );
    await settle();

    expect(page(fixture).querySelector('[data-testid="save-error"]')?.textContent).toContain('Spring Web');
  });
});