import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { FormArray } from "@angular/forms";
import { DependencyRows } from "./dependency-rows";
import { DependencyDefinition } from "../catalog.models";
import { dependencyForm, DependencyForm } from "../group-editor/group-form";

describe('DependencyRows', () => {
  let http: HttpTestingController;

  const starter: DependencyDefinition = {
    name: 'Spring Security', groupId: 'org.springframework.boot', artifactId: 'spring-boot-starter-security',
    version: null, kind: 'LIBRARY', mavenScope: 'COMPILE', gradleScope: 'IMPLEMENTATION',
    includedByDefault: true, reactive: false,
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

  async function render(rows: FormArray<DependencyForm>) {
    const fixture = TestBed.createComponent(DependencyRows);
    fixture.componentRef.setInput('rows', rows);
    TestBed.tick();
    await settle();
    return fixture;
  }

  const page = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;

  it('pinta una fila por dependencia y consulta sus versiones', async () => {
    const rows = new FormArray([dependencyForm(starter)]);
    const fixture = await render(rows);

    const call = http.expectOne('/api/v1/versions');
    expect(call.request.body).toEqual({
      artifacts: [{ groupId: 'org.springframework.boot', artifactId: 'spring-boot-starter-security' }],
    });
    call.flush({ artifacts: [] });
    await settle();

    expect(page(fixture).querySelectorAll('[data-testid^="dependency-"]').length).toBe(1);
  });

  it('agrega filas en la lista recibida y no deja quitar la última', async () => {
    const rows = new FormArray([dependencyForm()]);
    const fixture = await render(rows);
    const remove = () => page(fixture).querySelector<HTMLButtonElement>('[data-testid="remove-0"]')!;

    expect(remove().disabled).toBe(true);

    page(fixture).querySelector<HTMLButtonElement>('[data-testid="add-dependency"]')!.click();
    await settle();

    expect(rows.length).toBe(2);
    expect(remove().disabled).toBe(false);
  });
});