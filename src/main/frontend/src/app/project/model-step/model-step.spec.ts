import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { ModelStep, VALIDATION_DELAY_MS } from "./model-step";
import { modelForm, NO_CONSTRAINTS } from "../../model/model-form";
import { Attribute, DomainModel, EMPTY_DOMAIN_MODEL, Entity, ModelValidationResult } from "../../model/model.models";

describe('ModelStep', () => {
  let http: HttpTestingController;

  const attribute = (id: string, name: string): Attribute => ({
    id,
    name,
    columnName: null,
    type: { scalar: 'STRING', enumId: null },
    required: true,
    unique: false,
    constraints: NO_CONSTRAINTS,
  });

  const entity = (id: string, name: string, extra: Partial<Entity> = {}): Entity => ({
    id,
    name,
    tableName: null,
    resourcePath: null,
    sequenceName: null,
    superclassId: null,
    attributes: [],
    relations: [],
    ...extra,
  });

  /** Cliente y Pedido, con Pedido N‑1 Cliente; Auditable (activo) ← Documento. */
  const model: DomainModel = {
    ...EMPTY_DOMAIN_MODEL,
    superclasses: [
      { id: 's-auditable', name: 'Auditable', parentId: null, attributes: [attribute('a-activo', 'activo')] },
      { id: 's-documento', name: 'Documento', parentId: 's-auditable', attributes: [] },
    ],
    entities: [
      entity('e-cliente', 'Cliente', { attributes: [attribute('a-nombre', 'nombre')] }),
      entity('e-pedido', 'Pedido', {
        superclassId: 's-documento',
        relations: [
          {
            id: 'r-cliente',
            name: 'cliente',
            targetEntityId: 'e-cliente',
            cardinality: 'MANY_TO_ONE',
            required: true,
            inverseName: null,
            joinColumn: null,
            joinTable: null,
          },
        ],
      }),
    ],
  };

  const valid: ModelValidationResult = { valid: true, issues: [], entities: [], superclasses: [], relations: [] };

  const withNameError: ModelValidationResult = {
    ...valid,
    valid: false,
    issues: [{ severity: 'ERROR', elementId: 'a-nombre', location: 'Cliente › nombre', message: 'Nombre repetido' }],
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

  /** Espera la pausa de validación y contesta todas las validaciones pendientes. */
  async function validated(result: ModelValidationResult = valid): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve, VALIDATION_DELAY_MS + 50));
    TestBed.tick();
    for (const call of http.match('/api/v1/models/validate')) {
      call.flush(result);
    }
    await settle();
  }

  async function render(initial?: DomainModel, engines: readonly string[] = [], result = valid) {
    const form = modelForm(initial);
    const fixture = TestBed.createComponent(ModelStep);
    fixture.componentRef.setInput('form', form);
    fixture.componentRef.setInput('enginesWithoutSequences', engines);
    await settle();
    await validated(result);
    return { fixture, form };
  }

  const page = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;
  const byTestId = <T extends HTMLElement = HTMLElement>(fixture: { nativeElement: unknown }, testId: string) =>
    page(fixture).querySelector<T>(`[data-testid="${testId}"]`);

  async function click(element: HTMLElement | null): Promise<void> {
    element!.click();
    await settle();
  }

  async function openTab(fixture: { nativeElement: unknown }, label: string): Promise<void> {
    await click([...page(fixture).querySelectorAll<HTMLElement>('.mdc-tab')].find((tab) =>
      tab.textContent?.includes(label)) ?? null);
  }

  async function selectEntity(fixture: { nativeElement: unknown }, id: string): Promise<void> {
    await click(page(fixture).querySelector<HTMLElement>(`[data-testid="entity-${id}"] .item-name`));
  }

  async function lastValidity(fixture: { componentInstance: ModelStep }): Promise<boolean | undefined> {
    let last: boolean | undefined;
    fixture.componentInstance.validityChange.subscribe((value) => (last = value));
    await settle();
    return last;
  }

  it('sin elementos invita a crear; al agregar una entidad la elige y edita su nombre', async () => {
    const { fixture, form } = await render();
    expect(byTestId(fixture, 'detail-empty')).not.toBeNull();

    await click(byTestId(fixture, 'add-entity'));
    const name = byTestId<HTMLInputElement>(fixture, 'entity-name')!;
    name.value = 'Cliente';
    name.dispatchEvent(new Event('input'));
    await settle();

    expect(form.controls.entities.length).toBe(1);
    expect(form.controls.entities.at(0).controls.name.value).toBe('Cliente');
    expect(page(fixture).querySelector('.lists')?.textContent).toContain('Cliente');
    await validated();
  });

  it('muestra en gris los nombres derivados que calcula el servidor', async () => {
    const { fixture } = await render(model, [], {
      ...valid,
      entities: [
        {
          entityId: 'e-cliente',
          tableName: 'cliente',
          resourcePath: '/clientes',
          sequenceName: 'cliente_seq',
          columns: {},
          inverses: [],
        },
      ],
    });

    await selectEntity(fixture, 'e-cliente');

    expect(byTestId<HTMLInputElement>(fixture, 'entity-table')!.placeholder).toBe('cliente');
    expect(byTestId<HTMLInputElement>(fixture, 'entity-path')!.placeholder).toBe('/clientes');
    expect(byTestId(fixture, 'entity-sequence')).toBeNull();
  });

  it('al elegir UUID como tipo cambia a una estrategia compatible', async () => {
    const { form } = await render();

    document.querySelector<HTMLElement>('[data-testid="id-type"] .mat-mdc-select-trigger')!.click();
    await settle();
    [...document.querySelectorAll<HTMLElement>('mat-option')].find((o) => o.textContent?.trim() === 'UUID')!.click();
    await settle();

    expect(form.controls.idSettings.controls.type.value).toBe('UUID');
    expect(form.controls.idSettings.controls.generation.value).toBe('UUID');
    await validated();
  });

  it('con Secuencia y un motor sin secuencias muestra el error nombrando el motor', async () => {
    const withSequences: DomainModel = {
      ...model,
      idSettings: { type: 'LONG', generation: 'SEQUENCE', placement: 'ENTITY' },
    };
    const { fixture } = await render(withSequences, ['MySQL']);

    expect(byTestId(fixture, 'sequence-warning')?.textContent).toContain('MySQL');
  });

  it('cada elemento cuenta los problemas de sus atributos y relaciones', async () => {
    const { fixture } = await render(model, [], {
      ...valid,
      valid: false,
      issues: [
        { severity: 'ERROR', elementId: 'a-nombre', location: 'Cliente › nombre', message: 'Mal' },
        { severity: 'WARNING', elementId: 'e-cliente', location: 'Cliente', message: 'Ojo' },
        { severity: 'ERROR', elementId: null, location: 'Id', message: 'Id inválido' },
      ],
    });

    expect(byTestId(fixture, 'errors-e-cliente')?.textContent).toContain('1');
    expect(byTestId(fixture, 'warnings-e-cliente')?.textContent).toContain('1');
    expect(byTestId(fixture, 'errors-e-pedido')).toBeNull();
    expect(byTestId(fixture, 'global-issues')?.textContent).toContain('Id inválido');
  });

  it('borrar una entidad pide confirmación y avisa las relaciones que se borran con ella', async () => {
    const { fixture, form } = await render(model);

    await click(byTestId(fixture, 'delete-entity-e-cliente'));
    expect(byTestId(fixture, 'confirm-delete')?.textContent).toContain('1 relación');

    await click(byTestId(fixture, 'confirm-delete-yes'));

    expect(form.controls.entities.length).toBe(1);
    expect(form.controls.entities.at(0).controls.relations.length).toBe(0);
    expect(byTestId(fixture, 'confirm-delete')).toBeNull();
    await validated();
  });

  it('cancelar el borrado no cambia nada', async () => {
    const { fixture, form } = await render(model);

    await click(byTestId(fixture, 'delete-superclass-s-auditable'));
    expect(byTestId(fixture, 'confirm-delete')?.textContent).toContain('quedarán sin superclase');
    await click(byTestId(fixture, 'confirm-delete-no'));

    expect(form.controls.superclasses.length).toBe(2);
  });

  it('las superclases se muestran en árbol y las entidades indican de quién heredan', async () => {
    const { fixture } = await render(model);

    const items = [...page(fixture).querySelectorAll<HTMLElement>('[data-testid^="superclass-s-"]')];
    expect(items.map((item) => item.dataset['testid'])).toEqual(['superclass-s-auditable', 'superclass-s-documento']);
    expect(items.map((item) => item.dataset['depth'])).toEqual(['0', '1']);
    expect(byTestId(fixture, 'entity-e-pedido')?.textContent).toContain('hereda de Documento');
  });

  it('la pestaña Atributos de una entidad lista sus atributos con la columna derivada', async () => {
    const { fixture } = await render(model, [], {
      ...valid,
      entities: [
        {
          entityId: 'e-cliente',
          tableName: 'cliente',
          resourcePath: '/clientes',
          sequenceName: 'cliente_seq',
          columns: { 'a-nombre': 'nombre' },
          inverses: [],
        },
      ],
    });

    await selectEntity(fixture, 'e-cliente');
    await openTab(fixture, 'Atributos (1)');

    expect(byTestId<HTMLInputElement>(fixture, 'attribute-column-a-nombre')!.placeholder).toBe('nombre');
  });

  it('la pestaña Atributos de una superclase usa sus columnas derivadas', async () => {
    const { fixture } = await render(model, [], {
      ...valid,
      superclasses: [{ superclassId: 's-auditable', columns: { 'a-activo': 'activo' } }],
    });

    await click(page(fixture).querySelector<HTMLElement>('[data-testid="superclass-s-auditable"] .item-name'));
    await openTab(fixture, 'Atributos (1)');

    expect(byTestId<HTMLInputElement>(fixture, 'attribute-column-a-activo')!.placeholder).toBe('activo');
  });

  it('la pestaña Relaciones muestra la FK derivada y, en el destino, el inverso que llega', async () => {
    const { fixture } = await render(model, [], {
      ...valid,
      entities: [
        {
          entityId: 'e-cliente',
          tableName: 'cliente',
          resourcePath: '/clientes',
          sequenceName: 'cliente_seq',
          columns: {},
          inverses: [{ name: 'pedidos', sourceEntityId: 'e-pedido', relationId: 'r-cliente', cardinality: 'ONE_TO_MANY' }],
        },
      ],
      relations: [
        { relationId: 'r-cliente', ownerEntityId: 'e-pedido', joinColumn: 'cliente_id', joinTable: null, inverseJoinColumn: null },
      ],
    });

    await selectEntity(fixture, 'e-pedido');
    await openTab(fixture, 'Relaciones (1)');
    expect(byTestId<HTMLInputElement>(fixture, 'relation-column-r-cliente')!.placeholder).toBe('cliente_id');
    expect(byTestId(fixture, 'relation-fk-hint-r-cliente')?.textContent).toContain('tabla de Pedido');

    await selectEntity(fixture, 'e-cliente');
    await openTab(fixture, 'Relaciones (0)');
    expect(byTestId(fixture, 'incoming')?.textContent).toContain('Pedido › cliente → pedidos');
  });

  it('el panel Problemas cuenta errores y lleva al atributo abriendo su pestaña', async () => {
    const { fixture } = await render(model, [], withNameError);
    expect(byTestId(fixture, 'problems')?.textContent).toContain('1 error');
    expect(byTestId(fixture, 'detail-empty')).not.toBeNull();

    await click(byTestId(fixture, 'problem-0'));
    await settle();

    expect(byTestId(fixture, 'entity-e-cliente')?.classList).toContain('selected');
    expect(byTestId(fixture, 'attribute-name-a-nombre')).not.toBeNull();
  });

  it('informa al asistente si el modelo permite avanzar', async () => {
    const { fixture } = await render(model, [], withNameError);
    expect(await lastValidity(fixture)).toBe(false);
  });

  it('un modelo sin errores permite avanzar salvo que falten secuencias', async () => {
    const ok = await render(model);
    expect(await lastValidity(ok.fixture)).toBe(true);

    const withSequences: DomainModel = {
      ...model,
      idSettings: { type: 'LONG', generation: 'SEQUENCE', placement: 'ENTITY' },
    };
    const blocked = await render(withSequences, ['MySQL']);
    expect(await lastValidity(blocked.fixture)).toBe(false);
  });
});