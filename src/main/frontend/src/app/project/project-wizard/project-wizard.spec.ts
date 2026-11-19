import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { ProjectWizard } from './project-wizard';
import { VALIDATION_DELAY_MS } from "../model-step/model-step";

describe('ProjectWizard', () => {
  let http: HttpTestingController;

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

  /**
   * Crea el asistente y responde las cargas que piden los pasos al
   * construirse: arquitecturas, motores, seguridad, grupos del modelo,
   * catálogo, la primera resolución y la primera validación del modelo.
   * Hasta responderlas, la aplicación no está estable y `whenStable()` no termina.
   */
  async function render() {
    const fixture = TestBed.createComponent(ProjectWizard);
    TestBed.tick();
    await settle();
    for (const call of http.match('/api/v1/architectures')) {
      call.flush({ styles: [], layouts: [], patterns: [] });
    }
    for (const call of http.match('/api/v1/databases')) {
      call.flush([]);
    }
    for (const call of http.match('/api/v1/security/mechanisms')) {
      call.flush([]);
    }
    for (const call of http.match('/api/v1/security/identity-providers')) {
      call.flush([]);
    }
    for (const call of http.match('/api/v1/models/dependency-groups')) {
      call.flush({ persistenceGroupId: 'spring-data', validationGroupId: 'validation' });
    }
    for (const call of http.match('/api/v1/catalog')) {
      call.flush({ groups: [] });
    }
    await settle();
    for (const call of http.match('/api/v1/catalog/resolve')) {
      call.flush({ groupIds: [], dependencies: [] });
    }
    await new Promise((resolve) => setTimeout(resolve, VALIDATION_DELAY_MS + 50));
    TestBed.tick();
    for (const call of http.match('/api/v1/models/validate')) {
      call.flush({ valid: true, issues: [], entities: [], superclasses: [], relations: [] });
    }
    await settle();
    return fixture;
  }

  const page = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;
  const selectedStep = (fixture: { nativeElement: unknown }) =>
    page(fixture).querySelector('.mat-step-header[aria-selected="true"]')?.textContent ?? '';

  function type(input: HTMLInputElement, value: string): void {
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  it('empieza en el paso Proyecto y muestra el paso Arquitectura', async () => {
    const fixture = await render();

    expect(selectedStep(fixture)).toContain('Proyecto');
    expect(page(fixture).textContent).toContain('Arquitectura');
  });

  it('muestra el paquete base y lo actualiza al escribir', async () => {
    const fixture = await render();
    expect(page(fixture).querySelector('[data-testid="base-package"]')?.textContent).toContain('com.example.demo');

    type(page(fixture).querySelector<HTMLInputElement>('[data-testid="artifact-id"]')!, 'api-clientes');
    await fixture.whenStable();

    expect(page(fixture).querySelector('[data-testid="base-package"]')?.textContent)
      .toContain('com.example.apiclientes');
  });

  it('no avanza mientras el paso Proyecto tenga errores', async () => {
    const fixture = await render();
    type(page(fixture).querySelector<HTMLInputElement>('[data-testid="group-id"]')!, 'Org.Jers');
    await fixture.whenStable();

    page(fixture).querySelector<HTMLButtonElement>('[data-testid="next-basics"]')!.click();
    await fixture.whenStable();

    expect(selectedStep(fixture)).toContain('Proyecto');
  });

  it('avanza a Arquitectura con el paso Proyecto válido', async () => {
    const fixture = await render();

    page(fixture).querySelector<HTMLButtonElement>('[data-testid="next-basics"]')!.click();
    await fixture.whenStable();

    expect(selectedStep(fixture)).toContain('Arquitectura');
  });

  it('tiene los ocho pasos en orden, con Modelo entre Seguridad y Dependencias', async () => {
    const fixture = await render();

    const labels = [...page(fixture).querySelectorAll('.mat-step-label')].map((label) => label.textContent?.trim());
    expect(labels).toEqual([
      'Proyecto', 'Arquitectura', 'Bases de datos', 'Seguridad', 'Modelo', 'Dependencias', 'Opciones', 'Resumen',
    ]);
  });

  it('con un modelo válido no muestra el aviso de errores', async () => {
    const fixture = await render();

    expect(page(fixture).querySelector('[data-testid="model-invalid"]')).toBeNull();
  });
});