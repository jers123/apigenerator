import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { GroupEditor } from './group-editor';
import { groupForm, GroupForm, STANDALONE_CATEGORIES, toGroupDefinition } from "./group-form";
import { DependencyCategory, GroupDefinition } from "../catalog.models";

describe('GroupEditor', () => {
  let http: HttpTestingController;

  const lombokGroup: GroupDefinition = {
    id: 'lombok',
    name: 'Lombok',
    category: 'OTHER',
    selectedByDefault: false,
    requiresDatabase: false,
    dependencies: [{
      name: 'Lombok', groupId: 'org.projectlombok', artifactId: 'lombok', version: '1.18.40', kind: 'LIBRARY',
      mavenScope: 'PROVIDED', gradleScope: 'COMPILE_ONLY', includedByDefault: true, reactive: false,
    }],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  /** Deja terminar las microtareas de los resource y ejecuta effects y detección de cambios. */
  async function settle(): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve));
    TestBed.tick();
  }

  async function render(
    form: GroupForm,
    categories: readonly DependencyCategory[] = STANDALONE_CATEGORIES,
    idLocked = false,
  ) {
    const fixture = TestBed.createComponent(GroupEditor);
    fixture.componentRef.setInput('form', form);
    fixture.componentRef.setInput('categories', categories);
    fixture.componentRef.setInput('idLocked', idLocked);
    TestBed.tick();
    await settle();
    return fixture;
  }

  const page = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;
  const versionsCall = () => http.expectOne('/api/v1/versions');
  const openedOptions = () => [...document.querySelectorAll('mat-option')].map((o) => o.textContent?.trim());

  async function openSelect(fixture: { whenStable(): Promise<unknown> }, testId: string) {
    document.querySelector<HTMLElement>(`[data-testid="${testId}"] .mat-mdc-select-trigger`)!.click();
    await fixture.whenStable();
  }

  it('consulta versiones y ofrece "Gestionada por Spring Boot", la guardada y las publicadas', async () => {
    const fixture = await render(groupForm('OTHER', lombokGroup));

    const call = versionsCall();
    expect(call.request.body).toEqual({ artifacts: [{ groupId: 'org.projectlombok', artifactId: 'lombok' }] });
    call.flush({ artifacts: [{ groupId: 'org.projectlombok', artifactId: 'lombok', latest: '1.18.42',
      versions: ['1.18.42'], available: true }] });
    await fixture.whenStable();
    await openSelect(fixture, 'version-0');

    expect(openedOptions()).toEqual(['Gestionada por Spring Boot', '1.18.40', '1.18.42']);
  });

  it('elegir "Gestionada por Spring Boot" envía la versión como null', async () => {
    const form = groupForm('OTHER', lombokGroup);
    const fixture = await render(form);
    versionsCall().flush({ artifacts: [{ groupId: 'org.projectlombok', artifactId: 'lombok', latest: '1.18.42',
      versions: ['1.18.42', '1.18.40'], available: true }] });
    await fixture.whenStable();
    await openSelect(fixture, 'version-0');

    document.querySelector<HTMLElement>('mat-option')!.click();
    await fixture.whenStable();

    expect(toGroupDefinition(form).dependencies[0].version).toBeNull();
  });

  it('sin conexión con Maven Central deja escribir la versión a mano', async () => {
    const form = groupForm('OTHER', lombokGroup);
    const fixture = await render(form);
    versionsCall().flush({ artifacts: [{ groupId: 'org.projectlombok', artifactId: 'lombok', latest: null,
      versions: [], available: false }] });
    await fixture.whenStable();

    const input = page(fixture).querySelector<HTMLInputElement>('[data-testid="version-0"]')!;
    expect(input.tagName).toBe('INPUT');
    expect(page(fixture).querySelector('[data-testid="dependency-0"]')?.textContent).toContain('Sin conexión');

    input.value = '1.18.50';
    input.dispatchEvent(new Event('input'));

    expect(toGroupDefinition(form).dependencies[0].version).toBe('1.18.50');
  });

  it('no consulta versiones mientras faltan coordenadas', async () => {
    await render(groupForm('SPRING'));

    http.expectNone('/api/v1/versions');
  });

  it('agrega y quita dependencias, pero nunca deja el grupo sin ninguna', async () => {
    const fixture = await render(groupForm('SPRING'));
    const rows = () => page(fixture).querySelectorAll('[data-testid^="dependency-"]').length;
    const remove = (i: number) => page(fixture).querySelector<HTMLButtonElement>(`[data-testid="remove-${i}"]`)!;

    expect(remove(0).disabled).toBe(true);

    page(fixture).querySelector<HTMLButtonElement>('[data-testid="add-dependency"]')!.click();
    await fixture.whenStable();
    expect(rows()).toBe(2);
    expect(remove(0).disabled).toBe(false);

    remove(1).click();
    await fixture.whenStable();
    expect(rows()).toBe(1);
  });

  it('muestra los ámbitos solo para librerías', async () => {
    const fixture = await render(groupForm('OTHER', lombokGroup));
    versionsCall().flush({ artifacts: [] });
    await fixture.whenStable();
    expect(page(fixture).querySelector('[data-testid="maven-scope-0"]')).not.toBeNull();

    await openSelect(fixture, 'kind-0');
    [...document.querySelectorAll<HTMLElement>('mat-option')].find((o) => o.textContent?.trim() === 'PLUGIN')!.click();
    await fixture.whenStable();

    expect(page(fixture).querySelector('[data-testid="maven-scope-0"]')).toBeNull();
    expect(page(fixture).querySelector('[data-testid="gradle-scope-0"]')).toBeNull();
  });

  it('al editar un grupo existente el id es de solo lectura', async () => {
    const fixture = await render(groupForm('OTHER', lombokGroup), STANDALONE_CATEGORIES, true);
    versionsCall().flush({ artifacts: [] });

    expect(page(fixture).querySelector<HTMLInputElement>('[data-testid="group-id"]')!.readOnly).toBe(true);
  });

  it('en modo base de datos la categoría se muestra fija', async () => {
    const fixture = await render(groupForm('DATABASE'), ['DATABASE']);

    expect(page(fixture).querySelector('[data-testid="group-category"]')).toBeNull();
    expect(page(fixture).querySelector('[data-testid="group-category-fixed"]')?.textContent).toContain('Database');
  });

  it('en modo independiente ofrece solo SPRING, OTHER, MAVEN y GRADLE', async () => {
    const fixture = await render(groupForm('SPRING'));

    await openSelect(fixture, 'group-category');

    expect(openedOptions()).toEqual(['Spring', 'Other', 'Maven', 'Gradle']);
  });
});