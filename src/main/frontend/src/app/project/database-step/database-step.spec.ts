import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { DatabaseStep } from './database-step';
import { DatabasesForm, projectForm, toConnectionRequest } from "../project-form";
import { DatabaseEngine } from "../../database/database.models";

describe('DatabaseStep', () => {
  let http: HttpTestingController;

  const postgresql: DatabaseEngine = {
    id: 'postgresql', label: 'PostgreSQL', driverGroupId: 'postgresql-driver', embedded: false, defaultPort: 5432,
    defaultUsername: 'postgres', jdbcUrlTemplate: 'jdbc:postgresql://{host}:{port}/{database}',
    kind: 'RELATIONAL', supportsSequences: true, exampleUrl: 'jdbc:postgresql://localhost:5432/mi_base',
  };
  const h2: DatabaseEngine = {
    id: 'h2', label: 'H2', driverGroupId: 'h2-database', embedded: true, defaultPort: null,
    defaultUsername: 'sa', jdbcUrlTemplate: 'jdbc:h2:mem:{database}',
    kind: 'RELATIONAL', supportsSequences: true, exampleUrl: 'jdbc:h2:mem:mi_base',
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

  async function render(form: DatabasesForm) {
    const fixture = TestBed.createComponent(DatabaseStep);
    fixture.componentRef.setInput('form', form);
    TestBed.tick();
    http.expectOne('/api/v1/databases').flush([postgresql, h2]);
    await settle();
    return fixture;
  }

  const page = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;
  const toggle = (fixture: { nativeElement: unknown }, testId: string) =>
    page(fixture).querySelector<HTMLButtonElement>(`[data-testid="${testId}"] button`)!;

  it('sin base de datos explica que los datos viven en memoria y el paso es válido', async () => {
    const form = projectForm().controls.databases;
    const fixture = await render(form);

    expect(page(fixture).querySelector('[data-testid="databases-info"]')?.textContent).toContain('memoria');
    expect(page(fixture).querySelector('[data-testid="database-engine"]')).toBeNull();
    expect(form.valid).toBe(true);
  });

  it('al activar la base de desarrollo propone el primer motor con sus valores y la URL', async () => {
    const form = projectForm().controls.databases;
    const fixture = await render(form);

    toggle(fixture, 'use-database').click();
    await fixture.whenStable();

    expect(form.controls.database.getRawValue()).toMatchObject({
      engineId: 'postgresql', host: 'localhost', port: 5432, username: 'postgres',
    });
    expect(page(fixture).querySelector('[data-testid="database-url"]')?.textContent)
      .toContain('jdbc:postgresql://localhost:5432/app');
    expect(form.valid).toBe(true);
  });

  it('la base de pruebas solo se activa con base de desarrollo y se apaga con ella', async () => {
    const form = projectForm().controls.databases;
    const fixture = await render(form);
    expect(toggle(fixture, 'use-test-database').disabled).toBe(true);

    toggle(fixture, 'use-database').click();
    await fixture.whenStable();
    toggle(fixture, 'use-test-database').click();
    await fixture.whenStable();
    expect(form.controls.useTestDatabase.value).toBe(true);

    toggle(fixture, 'use-database').click();
    await fixture.whenStable();

    expect(form.controls.useTestDatabase.value).toBe(false);
    expect(toConnectionRequest(form.controls.testDatabase)).toBeNull();
  });

  it('un motor embebido no pide servidor ni puerto', async () => {
    const form = projectForm().controls.databases;
    const fixture = await render(form);
    toggle(fixture, 'use-database').click();
    await fixture.whenStable();

    document.querySelector<HTMLElement>('[data-testid="database-engine"] .mat-mdc-select-trigger')!.click();
    await fixture.whenStable();
    [...document.querySelectorAll<HTMLElement>('mat-option')].find((o) => o.textContent?.includes('H2'))!.click();
    await fixture.whenStable();

    expect(page(fixture).querySelector('[data-testid="database-host"]')).toBeNull();
    expect(toConnectionRequest(form.controls.database)).toMatchObject({ engineId: 'h2', host: null, port: null });
  });
});