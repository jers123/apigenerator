import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { ArchitectureStep } from './architecture-step';
import { ArchitectureCatalog, ArchitectureStyle, Compatibility, DesignPatternInfo } from "../project.models";
import { ArchitectureForm, projectForm } from "../project-form";

/** Compatibilidad con todos los estilos: la indicada o, si no, compatible. */
function compatibility(overrides: Partial<Record<ArchitectureStyle, Compatibility>>): Record<ArchitectureStyle, Compatibility> {
  return {
    LAYERED: 'COMPATIBLE', PACKAGE_BY_FEATURE: 'COMPATIBLE', HEXAGONAL: 'COMPATIBLE', ONION: 'COMPATIBLE',
    CLEAN: 'COMPATIBLE', VERTICAL_SLICE: 'COMPATIBLE', MODULITH: 'COMPATIBLE', ...overrides,
  };
}

function pattern(id: string, label: string, extra: Partial<DesignPatternInfo> = {}): DesignPatternInfo {
  return {
    id, label, family: 'ENTERPRISE', familyLabel: 'Empresariales y de dominio', generatesCode: true,
    reason: null, compatibility: compatibility({}), ...extra,
  };
}

describe('ArchitectureStep', () => {
  let http: HttpTestingController;

  const catalog: ArchitectureCatalog = {
    styles: [
      { id: 'LAYERED', label: 'Capas', description: 'controller → service → repository', multiModule: true },
      { id: 'HEXAGONAL', label: 'Hexagonal', description: 'puertos y adaptadores', multiModule: true },
      { id: 'VERTICAL_SLICE', label: 'Vertical Slice', description: 'una carpeta por caso de uso', multiModule: false },
    ],
    layouts: [
      { id: 'SINGLE_MODULE', label: 'Un solo módulo (paquetes)' },
      { id: 'MULTI_MODULE', label: 'Varios módulos (subproyectos)' },
    ],
    patterns: [
      pattern('SERVICE_LAYER', 'Service Layer', {
        reason: 'capa compartida', compatibility: compatibility({ LAYERED: 'NATIVE', HEXAGONAL: 'NATIVE', VERTICAL_SLICE: 'CONTRAINDICATED' }),
      }),
      pattern('CQRS', 'CQRS', {
        family: 'ARCHITECTURAL', familyLabel: 'Arquitectónicos', reason: 'ya hay un único modelo',
        compatibility: compatibility({ LAYERED: 'CONTRAINDICATED', VERTICAL_SLICE: 'NATIVE' }),
      }),
      pattern('STRATEGY', 'Strategy', { family: 'BEHAVIORAL', familyLabel: 'De comportamiento (GoF)', generatesCode: false }),
      pattern('API_GATEWAY', 'API Gateway / BFF', {
        family: 'DISTRIBUTED', familyLabel: 'Distribuidos e integración', reason: 'enruta hacia varios servicios',
        compatibility: compatibility({ LAYERED: 'NOT_APPLICABLE', HEXAGONAL: 'NOT_APPLICABLE', VERTICAL_SLICE: 'NOT_APPLICABLE' }),
      }),
    ],
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

  async function render(form: ArchitectureForm) {
    const fixture = TestBed.createComponent(ArchitectureStep);
    fixture.componentRef.setInput('form', form);
    TestBed.tick();
    http.expectOne('/api/v1/architectures').flush(catalog);
    await settle();
    return fixture;
  }

  const page = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;
  const row = (fixture: { nativeElement: unknown }, id: string) =>
    page(fixture).querySelector<HTMLElement>(`[data-testid="pattern-${id}"]`)!;
  const checkbox = (fixture: { nativeElement: unknown }, id: string) =>
    row(fixture, id).querySelector<HTMLInputElement>('input[type="checkbox"]')!;

  it('muestra todos los patrones, también los que no se pueden elegir, con su marca', async () => {
    const fixture = await render(projectForm().controls.architecture);

    expect(row(fixture, 'SERVICE_LAYER').textContent).toContain('Incluido por el estilo');
    expect(checkbox(fixture, 'SERVICE_LAYER').checked).toBe(true);
    expect(checkbox(fixture, 'SERVICE_LAYER').disabled).toBe(true);

    expect(row(fixture, 'CQRS').textContent).toContain('Contraindicado');
    expect(row(fixture, 'CQRS').textContent).toContain('ya hay un único modelo');
    expect(checkbox(fixture, 'CQRS').disabled).toBe(true);

    expect(row(fixture, 'API_GATEWAY').textContent).toContain('No aplica');
    expect(row(fixture, 'STRATEGY').textContent).toContain('Opcional');
    expect(row(fixture, 'STRATEGY').textContent).toContain('solo documentación');
    expect(checkbox(fixture, 'STRATEGY').disabled).toBe(false);
  });

  it('explica las marcas en un aviso informativo', async () => {
    const fixture = await render(projectForm().controls.architecture);

    expect(page(fixture).querySelector('[data-testid="patterns-info"]')?.textContent).toContain('Contraindicado');
  });

  it('guarda en el formulario solo los patrones opcionales que marca el usuario', async () => {
    const form = projectForm().controls.architecture;
    const fixture = await render(form);

    checkbox(fixture, 'STRATEGY').click();
    await fixture.whenStable();

    expect(form.controls.patterns.value).toEqual(['STRATEGY']);
  });

  it('al cambiar de estilo quita los patrones que dejan de ser válidos y avisa cuáles', async () => {
    const form = projectForm().controls.architecture;
    form.patchValue({ style: 'HEXAGONAL', patterns: ['CQRS', 'STRATEGY'] });
    const fixture = await render(form);

    page(fixture).querySelector<HTMLInputElement>('[data-testid="style-LAYERED"] input')!.click();
    await fixture.whenStable();

    expect(form.controls.style.value).toBe('LAYERED');
    expect(form.controls.patterns.value).toEqual(['STRATEGY']);
    expect(page(fixture).querySelector('[data-testid="removed-notice"]')?.textContent).toContain('CQRS');
  });

  it('un estilo sin capas vuelve a un solo módulo y no permite varios', async () => {
    const form = projectForm().controls.architecture;
    form.patchValue({ layout: 'MULTI_MODULE' });
    const fixture = await render(form);

    page(fixture).querySelector<HTMLInputElement>('[data-testid="style-VERTICAL_SLICE"] input')!.click();
    await fixture.whenStable();

    expect(form.controls.layout.value).toBe('SINGLE_MODULE');
    expect(page(fixture).querySelector<HTMLButtonElement>('[data-testid="layout-MULTI_MODULE"] button')!.disabled)
      .toBe(true);
  });
});