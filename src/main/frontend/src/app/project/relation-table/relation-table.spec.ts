import { TestBed } from "@angular/core/testing";
import { FormArray } from "@angular/forms";
import { EntityOption, IncomingRelation, RelationTable } from "./relation-table";
import { CARDINALITIES, Cardinality, ModelIssue, Relation, RelationMapping } from "../../model/model.models";
import { relationForm, RelationForm } from "../../model/model-form";

describe('RelationTable', () => {
  const entities: readonly EntityOption[] = [
    { id: 'e-pedido', name: 'Pedido' },
    { id: 'e-cliente', name: 'Cliente' },
    { id: 'e-producto', name: 'Producto' },
  ];

  const relation = (id: string, name: string, target: string, cardinality: Cardinality, required = false): Relation => ({
    id,
    name,
    targetEntityId: target,
    cardinality,
    required,
    inverseName: null,
    joinColumn: null,
    joinTable: null,
  });

  const cliente = relation('r-cliente', 'cliente', 'e-cliente', 'MANY_TO_ONE', true);
  const productos = relation('r-productos', 'productos', 'e-producto', 'MANY_TO_MANY');

  const mappings: readonly RelationMapping[] = [
    { relationId: 'r-cliente', ownerEntityId: 'e-pedido', joinColumn: 'cliente_id', joinTable: null, inverseJoinColumn: null },
    {
      relationId: 'r-productos',
      ownerEntityId: 'e-pedido',
      joinColumn: 'pedido_id',
      joinTable: 'pedido_producto',
      inverseJoinColumn: 'producto_id',
    },
  ];

  async function settle(): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve));
    TestBed.tick();
  }

  async function render(
    relations: readonly Relation[],
    options: {
      entities?: readonly EntityOption[];
      incoming?: readonly IncomingRelation[];
      issues?: readonly ModelIssue[];
    } = {},
  ) {
    const array = new FormArray<RelationForm>(relations.map((r) => relationForm(r)));
    const fixture = TestBed.createComponent(RelationTable);
    fixture.componentRef.setInput('relations', array);
    fixture.componentRef.setInput('ownerId', 'e-pedido');
    fixture.componentRef.setInput('entities', options.entities ?? entities);
    fixture.componentRef.setInput('mappings', mappings);
    fixture.componentRef.setInput('incoming', options.incoming ?? []);
    fixture.componentRef.setInput('issues', options.issues ?? []);
    await settle();
    return { fixture, array };
  }

  const page = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;
  const byTestId = <T extends HTMLElement = HTMLElement>(fixture: { nativeElement: unknown }, testId: string) =>
    page(fixture).querySelector<T>(`[data-testid="${testId}"]`);

  async function click(element: HTMLElement | null): Promise<void> {
    element!.click();
    await settle();
  }

  async function choose(testId: string, optionText: string): Promise<void> {
    document.querySelector<HTMLElement>(`[data-testid="${testId}"] .mat-mdc-select-trigger`)!.click();
    await settle();
    [...document.querySelectorAll<HTMLElement>('mat-option')].find((o) => o.textContent?.trim() === optionText)!.click();
    await settle();
  }

  it('muestra la FK derivada en gris y en qué tabla queda', async () => {
    const { fixture } = await render([cliente]);

    expect(byTestId<HTMLInputElement>(fixture, 'relation-column-r-cliente')!.placeholder).toBe('cliente_id');
    expect(byTestId(fixture, 'relation-fk-hint-r-cliente')?.textContent).toContain('tabla de Pedido');
    expect(byTestId(fixture, 'relation-join-table-r-cliente')).toBeNull();
  });

  it('una N‑N muestra la tabla intermedia y la otra columna derivadas', async () => {
    const { fixture } = await render([productos]);

    expect(byTestId<HTMLInputElement>(fixture, 'relation-join-table-r-productos')!.placeholder).toBe('pedido_producto');
    expect(byTestId<HTMLInputElement>(fixture, 'relation-column-r-productos')!.placeholder).toBe('pedido_id');
    expect(byTestId(fixture, 'relation-other-column-r-productos')?.textContent).toContain('producto_id');
    expect(byTestId(fixture, 'relation-fk-hint-r-productos')).toBeNull();
  });

  it('una relación nueva es N‑1 hacia la primera entidad distinta', async () => {
    const { fixture, array } = await render([]);
    expect(byTestId(fixture, 'no-relations')).not.toBeNull();

    await click(byTestId(fixture, 'add-relation'));

    expect(array.length).toBe(1);
    expect(array.at(0).controls.targetEntityId.value).toBe('e-cliente');
    expect(array.at(0).controls.cardinality.value).toBe('MANY_TO_ONE');
  });

  it('si la entidad es la única, la relación nueva apunta a sí misma', async () => {
    const { fixture, array } = await render([], { entities: [{ id: 'e-pedido', name: 'Pedido' }] });

    await click(byTestId(fixture, 'add-relation'));

    expect(array.at(0).controls.targetEntityId.value).toBe('e-pedido');
  });

  it('al pasar a una colección deja de ser obligatoria y no se puede marcar', async () => {
    const { fixture, array } = await render([cliente]);
    const oneToMany = CARDINALITIES.find((c) => c.value === 'ONE_TO_MANY')!.label;

    await choose('relation-cardinality-r-cliente', oneToMany);

    expect(array.at(0).controls.cardinality.value).toBe('ONE_TO_MANY');
    expect(array.at(0).controls.required.value).toBe(false);
    expect(page(fixture).querySelector<HTMLInputElement>('[data-testid="relation-required-r-cliente"] input')!.disabled)
      .toBe(true);
  });

  it('marcar obligatoria actualiza el formulario', async () => {
    const { fixture, array } = await render([relation('r-1', 'jefe', 'e-cliente', 'MANY_TO_ONE')]);

    await click(page(fixture).querySelector<HTMLElement>('[data-testid="relation-required-r-1"] input'));

    expect(array.at(0).controls.required.value).toBe(true);
  });

  it('lista los inversos que llegan desde otras entidades', async () => {
    const { fixture } = await render([], {
      incoming: [
        {
          relationId: 'r-x',
          sourceName: 'Factura',
          relationName: 'pedido',
          inverseName: 'facturas',
          cardinality: 'ONE_TO_MANY',
        },
      ],
    });

    expect(byTestId(fixture, 'incoming')?.textContent).toContain('Factura › pedido → facturas');
  });

  it('muestra los problemas de la relación en su fila y la borra', async () => {
    const { fixture, array } = await render([cliente], {
      issues: [{ severity: 'ERROR', elementId: 'r-cliente', location: 'Pedido › cliente', message: 'Destino inválido' }],
    });

    const row = byTestId(fixture, 'relation-r-cliente')!;
    expect(row.textContent).toContain('Destino inválido');
    expect(row.classList).toContain('has-errors');

    await click(byTestId(fixture, 'relation-delete-r-cliente'));
    expect(array.length).toBe(0);
  });
});