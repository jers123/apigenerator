import { TestBed } from "@angular/core/testing";
import { FormArray } from "@angular/forms";
import { AttributeTable } from "./attribute-table";
import { Attribute, AttributeConstraints, ModelIssue, ScalarType } from "../../model/model.models";
import { attributeForm, AttributeForm, NO_CONSTRAINTS } from "../../model/model-form";

describe('AttributeTable', () => {
  const attribute = (
    id: string,
    name: string,
    scalar: ScalarType,
    constraints: AttributeConstraints = NO_CONSTRAINTS,
  ): Attribute => ({
    id,
    name,
    columnName: null,
    type: { scalar, enumId: null },
    required: false,
    unique: false,
    constraints,
  });

  const nombre = attribute('a-nombre', 'nombre', 'STRING', { ...NO_CONSTRAINTS, maxLength: 120, email: true });
  const total = attribute('a-total', 'total', 'BIG_DECIMAL');
  const hora = attribute('a-hora', 'hora', 'LOCAL_TIME');

  async function settle(): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve));
    TestBed.tick();
  }

  async function render(
    attributes: readonly Attribute[],
    columns: Readonly<Record<string, string>> = {},
    issues: readonly ModelIssue[] = [],
  ) {
    const array = new FormArray<AttributeForm>(attributes.map((a) => attributeForm(a)));
    const fixture = TestBed.createComponent(AttributeTable);
    fixture.componentRef.setInput('attributes', array);
    fixture.componentRef.setInput('columns', columns);
    fixture.componentRef.setInput('issues', issues);
    await settle();
    return { fixture, array };
  }

  const page = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;
  const byTestId = <T extends HTMLElement = HTMLElement>(root: ParentNode, testId: string) =>
    root.querySelector<T>(`[data-testid="${testId}"]`);

  async function click(element: HTMLElement | null): Promise<void> {
    element!.click();
    await settle();
  }

  it('muestra la columna derivada en gris y el resumen de las restricciones', async () => {
    const { fixture } = await render([nombre], { 'a-nombre': 'nombre' });

    expect(byTestId<HTMLInputElement>(page(fixture), 'attribute-column-a-nombre')!.placeholder).toBe('nombre');
    expect(byTestId(page(fixture), 'attribute-summary-a-nombre')?.textContent).toContain('máx. 120 car.');
    expect(byTestId(page(fixture), 'attribute-summary-a-nombre')?.textContent).toContain('email');
  });

  it('agrega un atributo String opcional al final', async () => {
    const { fixture, array } = await render([nombre]);

    await click(byTestId(page(fixture), 'add-attribute'));

    expect(array.length).toBe(2);
    expect(array.at(1).controls.type.value).toBe('STRING');
    expect(array.at(1).controls.required.value).toBe(false);
  });

  it('al cambiar el tipo quita las restricciones que el nuevo no admite', async () => {
    const { fixture, array } = await render([nombre]);

    document.querySelector<HTMLElement>('[data-testid="attribute-type-a-nombre"] .mat-mdc-select-trigger')!.click();
    await settle();
    [...document.querySelectorAll<HTMLElement>('mat-option')].find((o) => o.textContent?.trim() === 'Integer')!.click();
    await settle();

    expect(array.at(0).controls.type.value).toBe('INTEGER');
    expect(array.at(0).controls.constraints.getRawValue()).toEqual({ ...NO_CONSTRAINTS, pattern: '' });
    expect(byTestId(page(fixture), 'attribute-summary-a-nombre')).toBeNull();
  });

  it('un texto ofrece longitud, patrón y email, pero no rango', async () => {
    const { fixture } = await render([nombre]);

    await click(byTestId(page(fixture), 'attribute-toggle-a-nombre'));
    const panel = byTestId(page(fixture), 'attribute-constraints-a-nombre')!;

    expect(byTestId(panel, 'max-length')).not.toBeNull();
    expect(byTestId(panel, 'pattern')).not.toBeNull();
    expect(byTestId(panel, 'email')).not.toBeNull();
    expect(byTestId(panel, 'min')).toBeNull();
  });

  it('un BigDecimal ofrece rango y dígitos, pero no longitud', async () => {
    const { fixture, array } = await render([total]);

    await click(byTestId(page(fixture), 'attribute-toggle-a-total'));
    const panel = byTestId(page(fixture), 'attribute-constraints-a-total')!;
    const digits = byTestId<HTMLInputElement>(panel, 'integer-digits')!;
    digits.value = '10';
    digits.dispatchEvent(new Event('input'));
    await settle();

    expect(byTestId(panel, 'min')).not.toBeNull();
    expect(byTestId(panel, 'fraction-digits')).not.toBeNull();
    expect(byTestId(panel, 'max-length')).toBeNull();
    expect(array.at(0).controls.constraints.controls.integerDigits.value).toBe(10);
  });

  it('un tipo sin restricciones lo indica', async () => {
    const { fixture } = await render([hora]);

    await click(byTestId(page(fixture), 'attribute-toggle-a-hora'));

    expect(byTestId(page(fixture), 'no-constraints')).not.toBeNull();
  });

  it('muestra los problemas del atributo en su fila y la marca si son errores', async () => {
    const { fixture } = await render([nombre, total], {}, [
      { severity: 'ERROR', elementId: 'a-nombre', location: 'Cliente › nombre', message: 'Nombre repetido' },
      { severity: 'WARNING', elementId: 'e-cliente', location: 'Cliente', message: 'De la entidad' },
    ]);

    const row = byTestId(page(fixture), 'attribute-a-nombre')!;
    expect(row.textContent).toContain('Nombre repetido');
    expect(row.classList).toContain('has-errors');
    expect(byTestId(page(fixture), 'attribute-a-total')!.classList).not.toContain('has-errors');
    expect(page(fixture).textContent).not.toContain('De la entidad');
  });

  it('borra un atributo y, sin atributos, lo indica', async () => {
    const { fixture, array } = await render([nombre]);

    await click(byTestId(page(fixture), 'attribute-delete-a-nombre'));

    expect(array.length).toBe(0);
    expect(byTestId(page(fixture), 'no-attributes')).not.toBeNull();
  });
});