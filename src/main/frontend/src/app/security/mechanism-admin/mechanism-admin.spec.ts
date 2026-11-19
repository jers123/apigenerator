import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { MechanismAdmin } from "./mechanism-admin";
import { SecurityMechanismDefinition } from "../security.models";

describe('MechanismAdmin', () => {
  let http: HttpTestingController;

  const httpBasic: SecurityMechanismDefinition = {
    id: 'http-basic', name: 'HTTP Basic', description: 'Usuario y contraseña', role: 'API_AUTHENTICATION',
    reactiveSupported: true, requiresIssuer: false, managesUsers: true, conflictsWith: [],
    dependencies: [{ name: 'Spring Security', groupId: 'org.springframework.boot', artifactId: 'spring-boot-starter-security',
      version: null, kind: 'LIBRARY', mavenScope: 'COMPILE', gradleScope: 'IMPLEMENTATION', includedByDefault: true, reactive: false }],
  };
  const formLogin: SecurityMechanismDefinition = {
    ...httpBasic, id: 'form-login', name: 'Formulario', role: 'BROWSER_LOGIN', conflictsWith: ['http-basic'],
  };
  const url = '/api/v1/security/mechanisms';

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function settle(times = 3): Promise<void> {
    for (let i = 0; i < times; i++) {
      await new Promise((resolve) => setTimeout(resolve));
      TestBed.tick();
    }
  }

  const page = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;
  const click = (fixture: { nativeElement: unknown }, testId: string) =>
    page(fixture).querySelector<HTMLElement>(`[data-testid="${testId}"]`)!.click();

  function flushVersions(): void {
    for (const call of http.match('/api/v1/versions')) {
      call.flush({ artifacts: [] });
    }
  }

  async function render() {
    const fixture = TestBed.createComponent(MechanismAdmin);
    TestBed.tick();
    http.expectOne(url).flush([httpBasic, formLogin]);
    await settle();
    return fixture;
  }

  async function open(fixture: { nativeElement: unknown }, id: string) {
    click(fixture, `edit-${id}`);
    await settle();
    flushVersions();
    await settle();
  }

  it('lista los métodos con su tipo', async () => {
    const fixture = await render();

    expect(page(fixture).querySelector('[data-testid="edit-form-login"]')?.textContent)
      .toContain('Inicio de sesión en navegador');
  });

  it('edita un método y lo guarda con PUT sin alterarlo; después recarga la lista', async () => {
    const fixture = await render();
    await open(fixture, 'form-login');

    expect(page(fixture).querySelector<HTMLInputElement>('[data-testid="mechanism-id"]')!.readOnly).toBe(true);
    click(fixture, 'save');
    await settle(1);

    const call = http.expectOne(`${url}/form-login`);
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual(formLogin);
    call.flush(formLogin);
    await settle();
    http.expectOne(url).flush([httpBasic, formLogin]);
    await settle();

    expect(page(fixture).querySelector('[data-testid="notice"]')?.textContent).toContain('guardado');
  });

  it('el método no se ofrece como choque consigo mismo', async () => {
    const fixture = await render();
    await open(fixture, 'form-login');

    document.querySelector<HTMLElement>('[data-testid="mechanism-conflicts"] .mat-mdc-select-trigger')!.click();
    await settle();

    expect([...document.querySelectorAll('mat-option')].map((o) => o.textContent?.trim())).toEqual(['HTTP Basic']);
  });

  it('muestra el detalle cuando el servidor rechaza el cambio', async () => {
    const fixture = await render();
    await open(fixture, 'http-basic');

    click(fixture, 'save');
    await settle(1);
    http.expectOne(`${url}/http-basic`).flush(
      { title: 'Datos inválidos', status: 400, detail: "El método 'x' declara un choque con 'y', que no existe" },
      { status: 400, statusText: 'Bad Request' },
    );
    await settle();

    expect(page(fixture).querySelector('[data-testid="save-error"]')?.textContent).toContain('declara un choque');
  });

  it('no envía un método nuevo incompleto', async () => {
    const fixture = await render();
    click(fixture, 'new-mechanism');
    await settle();

    click(fixture, 'save');
    await settle();

    http.expectNone(url);
    expect(page(fixture).querySelector('[data-testid="save-error"]')?.textContent).toContain('Revisa');
  });
});