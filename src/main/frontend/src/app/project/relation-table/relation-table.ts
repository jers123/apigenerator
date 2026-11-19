import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormArray, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { map, startWith, switchMap } from 'rxjs';
import { CARDINALITIES, Cardinality, ModelIssue, RelationMapping } from '../../model/model.models';
import { applyCardinalityRules, isToMany, relationForm, RelationForm } from '../../model/model-form';

/** Entidad que se puede elegir como destino. */
export interface EntityOption {
  /** Id interno. */
  readonly id: string;
  /** Nombre visible. */
  readonly name: string;
}

/** Lado inverso que llega a la entidad desde una relación de otra. */
export interface IncomingRelation {
  /** Id interno de la relación que lo declara. */
  readonly relationId: string;
  /** Nombre de la entidad que declara la relación. */
  readonly sourceName: string;
  /** Nombre de la relación en esa entidad. */
  readonly relationName: string;
  /** Nombre del inverso en esta entidad. */
  readonly inverseName: string;
  /** Cardinalidad vista desde esta entidad. */
  readonly cardinality: Cardinality;
}

/**
 * Tabla editable de las relaciones que declara una entidad, con su mapeo
 * derivado y los inversos que le llegan desde otras entidades.
 */
@Component({
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  selector: 'app-relation-table',
  styleUrl: './relation-table.css',
  templateUrl: './relation-table.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RelationTable {
  /** Relaciones que declara la entidad. */
  readonly relations = input.required<FormArray<RelationForm>>();

  /** Id interno de la entidad que las declara. */
  readonly ownerId = input.required<string>();

  /** Entidades del modelo, posibles destinos. */
  readonly entities = input<readonly EntityOption[]>([]);

  /** Mapeo derivado de todas las relaciones, calculado por el servidor. */
  readonly mappings = input<readonly RelationMapping[]>([]);

  /** Inversos que llegan a la entidad desde otras. */
  readonly incoming = input<readonly IncomingRelation[]>([]);

  /** Problemas del modelo; la tabla muestra los de sus relaciones. */
  readonly issues = input<readonly ModelIssue[]>([]);

  /** Cardinalidades que se ofrecen. */
  protected readonly cardinalities = CARDINALITIES;

  /** Valor de la lista; se actualiza con cada cambio. */
  private readonly state = toSignal(
    toObservable(this.relations).pipe(
      switchMap((relations) => relations.valueChanges.pipe(startWith(null), map(() => relations.getRawValue()))),
    ),
  );

  /** Filas de la tabla, en orden. */
  protected readonly rows = computed<readonly RelationForm[]>(() => {
    this.state();
    return [...this.relations().controls];
  });

  /** Mapeo por id interno de relación. */
  private readonly mappingById = computed<ReadonlyMap<string, RelationMapping>>(
    () => new Map(this.mappings().map((mapping) => [mapping.relationId, mapping])),
  );

  /** Problemas por id interno de relación. */
  private readonly issuesById = computed<ReadonlyMap<string, readonly ModelIssue[]>>(() => {
    const grouped = new Map<string, ModelIssue[]>();
    for (const issue of this.issues()) {
      if (issue.elementId !== null) {
        grouped.set(issue.elementId, [...(grouped.get(issue.elementId) ?? []), issue]);
      }
    }
    return grouped;
  });

  /** Agrega una relación N‑1 hacia la primera entidad distinta, o hacia sí misma si es la única. */
  protected add(): void {
    const target = this.entities().find((entity) => entity.id !== this.ownerId())?.id ?? this.ownerId();
    this.relations().push(relationForm(undefined, target));
  }

  /**
   * Borra una relación.
   * @param index posición en la lista
   */
  protected remove(index: number): void {
    this.relations().removeAt(index);
  }

  /**
   * Al cambiar la cardinalidad, ajusta obligatoriedad y tabla intermedia.
   * @param relation fila de la relación
   */
  protected changeCardinality(relation: RelationForm): void {
    applyCardinalityRules(relation);
  }

  /**
   * Marca o desmarca la relación como obligatoria.
   * @param relation fila de la relación
   * @param checked nuevo estado
   */
  protected setRequired(relation: RelationForm, checked: boolean): void {
    relation.controls.required.setValue(checked);
  }

  /**
   * Indica si una cardinalidad es una colección.
   * @param cardinality cardinalidad
   * @returns `true` para 1‑N y N‑N
   */
  protected toMany(cardinality: Cardinality): boolean {
    return isToMany(cardinality);
  }

  /**
   * Nombre corto de una cardinalidad.
   * @param cardinality cardinalidad
   * @returns por ejemplo `1‑N`
   */
  protected cardinalityLabel(cardinality: Cardinality): string {
    return CARDINALITIES.find((option) => option.value === cardinality)?.label ?? cardinality;
  }

  /**
   * Mapeo derivado de una relación.
   * @param id id interno de la relación
   * @returns el mapeo, o `null` si aún no llega o su destino no existe
   */
  protected mappingOf(id: string): RelationMapping | null {
    return this.mappingById().get(id) ?? null;
  }

  /**
   * Explica en qué tabla queda la clave foránea de una relación sin tabla intermedia.
   * @param id id interno de la relación
   * @returns por ejemplo `Clave foránea en la tabla de Pedido`, o `null`
   */
  protected foreignKeyHint(id: string): string | null {
    const mapping = this.mappingOf(id);
    if (!mapping || mapping.joinTable !== null) {
      return null;
    }
    const owner = this.entities().find((entity) => entity.id === mapping.ownerEntityId)?.name ?? '?';
    return `Clave foránea en la tabla de ${owner}`;
  }

  /**
   * Problemas de una relación.
   * @param id id interno de la relación
   * @returns sus problemas, en orden
   */
  protected issuesOf(id: string): readonly ModelIssue[] {
    return this.issuesById().get(id) ?? [];
  }

  /**
   * Indica si una relación tiene errores.
   * @param id id interno de la relación
   * @returns `true` si alguno de sus problemas es error
   */
  protected hasErrors(id: string): boolean {
    return this.issuesOf(id).some((issue) => issue.severity === 'ERROR');
  }
}