import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormArray, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { map, startWith, switchMap } from 'rxjs';
import { addAttribute, AttributeForm, clearUnsupportedConstraints, constraintRules, ConstraintRules } from '../../model/model-form';
import { ModelIssue, SCALAR_TYPES, TEMPORAL_RULES } from '../../model/model.models';

/**
 * Tabla editable de los atributos de una entidad o superclase. Cada fila se
 * expande para editar las restricciones que admite su tipo.
 */
@Component({
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  selector: 'app-attribute-table',
  styleUrl: './attribute-table.css',
  templateUrl: './attribute-table.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AttributeTable {
  /** Atributos que se editan. */
  readonly attributes = input.required<FormArray<AttributeForm>>();

  /** Columna derivada por id interno de atributo, calculada por el servidor. */
  readonly columns = input<Readonly<Record<string, string>>>({});

  /** Problemas del modelo; la tabla muestra los de sus atributos. */
  readonly issues = input<readonly ModelIssue[]>([]);

  /** Tipos simples que se ofrecen. */
  protected readonly types = SCALAR_TYPES;

  /** Restricciones de tiempo que se ofrecen. */
  protected readonly temporalRules = TEMPORAL_RULES;

  /** Valor de la lista; se actualiza con cada cambio. */
  private readonly state = toSignal(
    toObservable(this.attributes).pipe(
      switchMap((attributes) => attributes.valueChanges.pipe(startWith(null), map(() => attributes.getRawValue()))),
    ),
  );

  /** Filas de la tabla, en orden. */
  protected readonly rows = computed<readonly AttributeForm[]>(() => {
    this.state();
    return [...this.attributes().controls];
  });

  /** Problemas por id interno de atributo. */
  private readonly issuesById = computed<ReadonlyMap<string, readonly ModelIssue[]>>(() => {
    const grouped = new Map<string, ModelIssue[]>();
    for (const issue of this.issues()) {
      if (issue.elementId !== null) {
        grouped.set(issue.elementId, [...(grouped.get(issue.elementId) ?? []), issue]);
      }
    }
    return grouped;
  });

  /** Id del atributo con las restricciones abiertas, o `null`. */
  protected readonly expanded = signal<string | null>(null);

  /** Agrega un atributo al final. */
  protected add(): void {
    addAttribute(this.attributes());
  }

  /**
   * Borra un atributo.
   * @param index posición en la lista
   */
  protected remove(index: number): void {
    const attributes = this.attributes();
    if (this.expanded() === attributes.at(index).controls.id.value) {
      this.expanded.set(null);
    }
    attributes.removeAt(index);
  }

  /**
   * Abre o cierra las restricciones de un atributo.
   * @param id id interno del atributo
   */
  protected toggle(id: string): void {
    this.expanded.update((current) => (current === id ? null : id));
  }

  /**
   * Al cambiar el tipo, quita las restricciones que el nuevo no admite.
   * @param attribute fila del atributo
   */
  protected changeType(attribute: AttributeForm): void {
    clearUnsupportedConstraints(attribute);
  }

  /**
   * Restricciones que admite el tipo de un atributo.
   * @param attribute fila del atributo
   * @returns grupos permitidos
   */
  protected rulesOf(attribute: AttributeForm): ConstraintRules {
    return constraintRules(attribute.controls.type.value);
  }

  /**
   * Indica si el tipo de un atributo admite alguna restricción.
   * @param rules grupos permitidos
   * @returns `true` si admite al menos uno
   */
  protected acceptsAny(rules: ConstraintRules): boolean {
    return rules.text || rules.range || rules.digits || rules.temporal;
  }

  /**
   * Columna derivada de un atributo, para mostrarla en gris.
   * @param id id interno del atributo
   * @returns la columna, o vacío si aún no llega
   */
  protected placeholderOf(id: string): string {
    return this.columns()[id] ?? '';
  }

  /**
   * Problemas de un atributo.
   * @param id id interno del atributo
   * @returns sus problemas, en orden
   */
  protected issuesOf(id: string): readonly ModelIssue[] {
    return this.issuesById().get(id) ?? [];
  }

  /**
   * Indica si un atributo tiene errores.
   * @param id id interno del atributo
   * @returns `true` si alguno de sus problemas es error
   */
  protected hasErrors(id: string): boolean {
    return this.issuesOf(id).some((issue) => issue.severity === 'ERROR');
  }

  /**
   * Resumen corto de las restricciones de un atributo.
   * @param attribute fila del atributo
   * @returns por ejemplo `máx. 120 car. · email`, o vacío si no tiene
   */
  protected summaryOf(attribute: AttributeForm): string {
    const c = attribute.controls.constraints.getRawValue();
    const parts: string[] = [];
    if (c.minLength !== null) {
      parts.push(`mín. ${c.minLength} car.`);
    }
    if (c.maxLength !== null) {
      parts.push(`máx. ${c.maxLength} car.`);
    }
    if (c.min !== null) {
      parts.push(`≥ ${c.min}`);
    }
    if (c.max !== null) {
      parts.push(`≤ ${c.max}`);
    }
    if (c.integerDigits !== null || c.fractionDigits !== null) {
      parts.push(`${c.integerDigits ?? '?'} enteros, ${c.fractionDigits ?? '?'} decimales`);
    }
    if (c.pattern.trim() !== '') {
      parts.push('patrón');
    }
    if (c.email) {
      parts.push('email');
    }
    if (c.temporal !== null) {
      parts.push(TEMPORAL_RULES.find((rule) => rule.value === c.temporal)?.label.toLowerCase() ?? '');
    }
    return parts.join(' · ');
  }
}