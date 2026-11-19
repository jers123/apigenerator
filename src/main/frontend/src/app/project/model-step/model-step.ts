import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal, signal } from '@angular/core';
import { outputFromObservable, rxResource, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { debounceTime, map, startWith, switchMap } from 'rxjs';
import { AttributeTable } from '../attribute-table/attribute-table';
import { IncomingRelation, RelationTable } from '../relation-table/relation-table';
import { ModelApi } from '../../model/model-api';
import { addEntity, addSuperclass, compatibleGenerations, EntityForm, ID_TYPES, ModelForm, parentCandidates, relationsPointingTo, removeEntity, removeSuperclass, SuperclassForm, toDomainModel } from '../../model/model-form';
import { EntityNames, ID_PLACEMENTS, ModelIssue, ModelValidationResult, RelationMapping, ScalarType } from '../../model/model.models';
import { problemMessage } from '../../shared/problem-detail';

/** Espera tras el último cambio antes de validar, para no llamar en cada tecla. */
export const VALIDATION_DELAY_MS = 400;

/** Elementos que se eligen en las listas. */
export type SelectableKind = 'superclass' | 'entity';

/** Valor completo del formulario del modelo. */
type ModelFormValue = ReturnType<ModelForm['getRawValue']>;

/** Sin columnas derivadas. */
const NO_COLUMNS: Readonly<Record<string, string>> = {};

/** Pestaña "General" del detalle. */
const GENERAL_TAB = 0;

/** Pestaña "Atributos" del detalle. */
const ATTRIBUTES_TAB = 1;

/** Pestaña "Relaciones" del detalle; solo en entidades. */
const RELATIONS_TAB = 2;

/** Elemento elegido en las listas. */
interface Selection {
  /** Tipo de elemento. */
  readonly kind: SelectableKind;
  /** Id interno. */
  readonly id: string;
}

/** Dónde se edita un elemento con id interno. */
interface Owner {
  /** Tipo de la superclase o entidad que lo contiene. */
  readonly kind: SelectableKind;
  /** Id interno de esa superclase o entidad. */
  readonly id: string;
  /** Pestaña del detalle donde se edita. */
  readonly tab: number;
}

/** Problemas de un elemento, contando sus atributos y relaciones. */
interface IssueCount {
  /** Errores. */
  readonly errors: number;
  /** Avisos. */
  readonly warnings: number;
}

/** Superclase en el árbol de la lista. */
interface TreeItem {
  /** Id interno. */
  readonly id: string;
  /** Nombre visible. */
  readonly name: string;
  /** Nivel en el árbol; 0 es raíz. */
  readonly depth: number;
}

/** Entidad en la lista. */
interface EntityItem {
  /** Id interno. */
  readonly id: string;
  /** Nombre visible. */
  readonly name: string;
  /** Nombre de su superclase, o `null` si no hereda. */
  readonly superclassName: string | null;
}

/** Opción de superclase para un selector. */
interface SuperclassOption {
  /** Id interno. */
  readonly id: string;
  /** Nombre visible. */
  readonly name: string;
}

/** Borrado que espera confirmación. */
interface PendingDelete {
  /** Tipo de elemento. */
  readonly kind: SelectableKind;
  /** Id interno. */
  readonly id: string;
  /** Qué se borra, por ejemplo `la entidad Cliente`. */
  readonly label: string;
  /** Consecuencias del borrado, o `null` si no tiene. */
  readonly detail: string | null;
}

/**
 * Nombre visible de un elemento.
 * @param name nombre escrito
 * @returns el nombre recortado, o `(sin nombre)`
 */
function displayName(name: string): string {
  return name.trim() === '' ? '(sin nombre)' : name.trim();
}

/**
 * Paso "Modelo" del asistente: configuración del id y la auditoría,
 * superclases y entidades. Valida el modelo en el servidor un momento
 * después de cada cambio, muestra los nombres derivados y los problemas, e
 * informa al asistente si se puede avanzar.
 */
@Component({
  imports: [ReactiveFormsModule, MatButtonModule, MatButtonToggleModule, MatCheckboxModule, MatFormFieldModule, MatInputModule, MatProgressBarModule, MatSelectModule, MatTabsModule, AttributeTable, RelationTable],
  selector: 'app-model-step',
  styleUrl: './model-step.css',
  templateUrl: './model-step.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModelStep {
  /** Formulario del modelo. */
  readonly form = input.required<ModelForm>();

  /** Nombres de los motores elegidos que no tienen secuencias. */
  readonly enginesWithoutSequences = input<readonly string[]>([]);

  /** Cliente de la API del modelo. */
  private readonly api = inject(ModelApi);

  /** Tipos que pueden ser id. */
  protected readonly idTypes = ID_TYPES;

  /** Ubicaciones del id. */
  protected readonly placements = ID_PLACEMENTS;

  /** Valor completo del formulario; se actualiza con cada cambio. */
  private readonly state = toSignal(
    toObservable(this.form).pipe(
      switchMap((form) => form.valueChanges.pipe(startWith(null), map((): ModelFormValue => form.getRawValue()))),
    ),
  );

  /** Modelo a validar; se emite un momento después del último cambio. */
  private readonly pendingModel = toSignal(
    toObservable(this.form).pipe(
      switchMap((form) =>
        form.valueChanges.pipe(
          startWith(null),
          debounceTime(VALIDATION_DELAY_MS),
          map(() => toDomainModel(form)),
        ),
      ),
    ),
  );

  /** Validación del modelo en el servidor. */
  protected readonly validation = rxResource({
    params: () => this.pendingModel(),
    stream: ({ params }) => this.api.validate(params),
  });

  /** Último resultado recibido; se conserva mientras llega el siguiente. */
  protected readonly result = linkedSignal<ModelValidationResult | undefined, ModelValidationResult | null>({
    source: () => (this.validation.hasValue() ? this.validation.value() : undefined),
    computation: (value, previous) => value ?? previous?.value ?? null,
  });

  /** Mensaje si la validación falló. */
  protected readonly validationError = computed(() => {
    const error = this.validation.error();
    return error ? problemMessage(error) : null;
  });

  /** Todos los problemas del último resultado. */
  protected readonly allIssues = computed<readonly ModelIssue[]>(() => this.result()?.issues ?? []);

  /** Cantidad de errores. */
  protected readonly errorCount = computed(() => this.allIssues().filter((issue) => issue.severity === 'ERROR').length);

  /** Cantidad de avisos. */
  protected readonly warningCount = computed(() => this.allIssues().length - this.errorCount());

  /** Mapeo derivado de todas las relaciones del último resultado. */
  protected readonly allMappings = computed<readonly RelationMapping[]>(() => this.result()?.relations ?? []);

  /** Estrategias compatibles con el tipo de id actual. */
  protected readonly generations = computed(() => compatibleGenerations(this.state()?.idSettings.type ?? 'LONG'));

  /** Estrategia de id actual. */
  protected readonly generation = computed(() => this.state()?.idSettings.generation ?? 'IDENTITY');

  /** Ubicación del id actual. */
  protected readonly placement = computed(() => this.state()?.idSettings.placement ?? 'ENTITY');

  /** Error de secuencias con los motores elegidos, o `null`. */
  protected readonly sequenceWarning = computed(() => {
    const engines = this.enginesWithoutSequences();
    if (this.generation() !== 'SEQUENCE' || engines.length === 0) {
      return null;
    }
    const verb = engines.length === 1 ? 'no las tiene' : 'no las tienen';
    return `La estrategia «Secuencia» necesita un motor con secuencias, y ${engines.join(', ')} ${verb}: `
      + 'use Autoincremento o cambie el motor en el paso Bases de datos.';
  });

  /** Si el modelo permite avanzar: sin errores del servidor ni de secuencias. */
  private readonly valid = computed(() => (this.result()?.valid ?? true) && this.sequenceWarning() === null);

  /** Avisa al asistente si el modelo permite avanzar. */
  readonly validityChange = outputFromObservable(toObservable(this.valid));

  /** Dónde se edita cada elemento con id interno. */
  private readonly owners = computed<ReadonlyMap<string, Owner>>(() => {
    const owners = new Map<string, Owner>();
    const state = this.state();
    for (const superclass of state?.superclasses ?? []) {
      owners.set(superclass.id, { kind: 'superclass', id: superclass.id, tab: GENERAL_TAB });
      for (const attribute of superclass.attributes) {
        owners.set(attribute.id, { kind: 'superclass', id: superclass.id, tab: ATTRIBUTES_TAB });
      }
    }
    for (const entity of state?.entities ?? []) {
      owners.set(entity.id, { kind: 'entity', id: entity.id, tab: GENERAL_TAB });
      for (const attribute of entity.attributes) {
        owners.set(attribute.id, { kind: 'entity', id: entity.id, tab: ATTRIBUTES_TAB });
      }
      for (const relation of entity.relations) {
        owners.set(relation.id, { kind: 'entity', id: entity.id, tab: RELATIONS_TAB });
      }
    }
    return owners;
  });

  /** Superclases en orden de árbol: cada raíz seguida de sus descendientes. */
  protected readonly superclassTree = computed<readonly TreeItem[]>(() => {
    const all = this.state()?.superclasses ?? [];
    const ids = new Set(all.map((superclass) => superclass.id));
    const visited = new Set<string>();
    const result: TreeItem[] = [];
    const visit = (parentId: string | null, depth: number): void => {
      for (const superclass of all) {
        const parent = superclass.parentId !== null && ids.has(superclass.parentId) ? superclass.parentId : null;
        if (parent === parentId && !visited.has(superclass.id)) {
          visited.add(superclass.id);
          result.push({ id: superclass.id, name: displayName(superclass.name), depth });
          visit(superclass.id, depth + 1);
        }
      }
    };
    visit(null, 0);
    for (const superclass of all) {
      if (!visited.has(superclass.id)) {
        result.push({ id: superclass.id, name: displayName(superclass.name), depth: 0 });
      }
    }
    return result;
  });

  /** Todas las superclases, para el selector "Hereda de" de una entidad. */
  protected readonly superclassOptions = computed<readonly SuperclassOption[]>(() =>
    (this.state()?.superclasses ?? []).map((superclass) => ({
      id: superclass.id,
      name: displayName(superclass.name),
    })),
  );

  /** Entidades de la lista; también son los destinos posibles de las relaciones. */
  protected readonly entityItems = computed<readonly EntityItem[]>(() => {
    const names = new Map(this.superclassOptions().map((option) => [option.id, option.name]));
    return (this.state()?.entities ?? []).map((entity) => ({
      id: entity.id,
      name: displayName(entity.name),
      superclassName: entity.superclassId === null ? null : (names.get(entity.superclassId) ?? null),
    }));
  });

  /** Elemento elegido, o `null`. */
  protected readonly selected = signal<Selection | null>(null);

  /** Pestaña abierta en el detalle. */
  protected readonly selectedTab = signal(GENERAL_TAB);

  /** Formulario de la entidad elegida, o `null`. */
  protected readonly selectedEntity = computed<EntityForm | null>(() => {
    this.state();
    const selection = this.selected();
    if (selection?.kind !== 'entity') {
      return null;
    }
    return this.form().controls.entities.controls.find((entity) => entity.controls.id.value === selection.id) ?? null;
  });

  /** Formulario de la superclase elegida, o `null`. */
  protected readonly selectedSuperclass = computed<SuperclassForm | null>(() => {
    this.state();
    const selection = this.selected();
    if (selection?.kind !== 'superclass') {
      return null;
    }
    return (
      this.form().controls.superclasses.controls.find((superclass) => superclass.controls.id.value === selection.id)
      ?? null
    );
  });

  /** Superclases que puede elegir como padre la superclase elegida. */
  protected readonly parentOptions = computed<readonly SuperclassOption[]>(() => {
    this.state();
    const selection = this.selected();
    if (selection?.kind !== 'superclass') {
      return [];
    }
    return parentCandidates(this.form(), selection.id).map((superclass) => ({
      id: superclass.id,
      name: displayName(superclass.name),
    }));
  });

  /** Nombres derivados de la entidad elegida, o `null` si aún no llegan. */
  protected readonly selectedNames = computed<EntityNames | null>(() => {
    const selection = this.selected();
    if (selection?.kind !== 'entity') {
      return null;
    }
    return this.result()?.entities.find((names) => names.entityId === selection.id) ?? null;
  });

  /** Columnas derivadas de los atributos del elemento elegido. */
  protected readonly selectedColumns = computed<Readonly<Record<string, string>>>(() => {
    const selection = this.selected();
    if (selection?.kind === 'entity') {
      return this.selectedNames()?.columns ?? NO_COLUMNS;
    }
    if (selection?.kind === 'superclass') {
      return this.result()?.superclasses.find((names) => names.superclassId === selection.id)?.columns ?? NO_COLUMNS;
    }
    return NO_COLUMNS;
  });

  /** Inversos que llegan a la entidad elegida, con los nombres de origen. */
  protected readonly incoming = computed<readonly IncomingRelation[]>(() => {
    const names = this.selectedNames();
    if (!names) {
      return [];
    }
    const entities = this.state()?.entities ?? [];
    return names.inverses.map((inverse) => {
      const source = entities.find((entity) => entity.id === inverse.sourceEntityId);
      const relation = source?.relations.find((candidate) => candidate.id === inverse.relationId);
      return {
        relationId: inverse.relationId,
        sourceName: displayName(source?.name ?? ''),
        relationName: displayName(relation?.name ?? ''),
        inverseName: inverse.name,
        cardinality: inverse.cardinality,
      };
    });
  });

  /** Problemas del elemento elegido, sin contar sus atributos ni relaciones. */
  protected readonly selectedIssues = computed<readonly ModelIssue[]>(() => {
    const id = this.selected()?.id;
    return id ? this.allIssues().filter((issue) => issue.elementId === id) : [];
  });

  /** Problemas del modelo en general, como los del id. */
  protected readonly globalIssues = computed<readonly ModelIssue[]>(() =>
    this.allIssues().filter((issue) => issue.elementId === null),
  );

  /** Problemas por superclase o entidad, contando sus atributos y relaciones. */
  protected readonly issueCounts = computed<ReadonlyMap<string, IssueCount>>(() => {
    const counts = new Map<string, IssueCount>();
    for (const issue of this.allIssues()) {
      const owner = this.ownerOf(issue);
      if (!owner) {
        continue;
      }
      const count = counts.get(owner.id) ?? { errors: 0, warnings: 0 };
      counts.set(
        owner.id,
        issue.severity === 'ERROR'
          ? { ...count, errors: count.errors + 1 }
          : { ...count, warnings: count.warnings + 1 },
      );
    }
    return counts;
  });

  /** Borrado que espera confirmación, o `null`. */
  protected readonly pendingDelete = signal<PendingDelete | null>(null);

  /**
   * Dónde se edita el elemento de un problema.
   * @param issue problema del modelo
   * @returns la superclase o entidad y la pestaña, o `undefined` si es del modelo en general
   */
  protected ownerOf(issue: ModelIssue): Owner | undefined {
    return issue.elementId === null ? undefined : this.owners().get(issue.elementId);
  }

  /**
   * Lleva al elemento de un problema: lo elige y abre su pestaña.
   * @param issue problema del panel
   */
  protected goTo(issue: ModelIssue): void {
    const owner = this.ownerOf(issue);
    if (!owner) {
      return;
    }
    this.pendingDelete.set(null);
    this.select(owner.kind, owner.id);
    this.selectedTab.set(owner.tab);
  }

  /**
   * Indica si un elemento es el elegido.
   * @param kind tipo de elemento
   * @param id id interno
   * @returns `true` si está elegido
   */
  protected isSelected(kind: SelectableKind, id: string): boolean {
    const selection = this.selected();
    return selection?.kind === kind && selection.id === id;
  }

  /**
   * Elige un elemento para editarlo.
   * @param kind tipo de elemento
   * @param id id interno
   */
  protected select(kind: SelectableKind, id: string): void {
    this.selected.set({ kind, id });
  }

  /**
   * Al cambiar el tipo de id, pasa a una estrategia compatible si la actual no lo es.
   * @param type tipo elegido
   */
  protected chooseIdType(type: ScalarType): void {
    const generation = this.form().controls.idSettings.controls.generation;
    const compatible = compatibleGenerations(type);
    if (!compatible.some((option) => option.value === generation.value) && compatible.length > 0) {
      generation.setValue(compatible[0].value);
    }
  }

  /** Agrega una entidad, la elige y abre su pestaña General. */
  protected newEntity(): void {
    const entity = addEntity(this.form());
    this.pendingDelete.set(null);
    this.select('entity', entity.controls.id.value);
    this.selectedTab.set(GENERAL_TAB);
  }

  /** Agrega una superclase raíz, la elige y abre su pestaña General. */
  protected newSuperclass(): void {
    const superclass = addSuperclass(this.form());
    this.pendingDelete.set(null);
    this.select('superclass', superclass.controls.id.value);
    this.selectedTab.set(GENERAL_TAB);
  }

  /**
   * Pide confirmación para borrar un elemento, explicando sus consecuencias.
   * @param kind tipo de elemento
   * @param id id interno
   */
  protected askDelete(kind: SelectableKind, id: string): void {
    const state = this.state();
    if (kind === 'entity') {
      const entity = state?.entities.find((candidate) => candidate.id === id);
      const count = relationsPointingTo(this.form(), id);
      const detail =
        count === 0
          ? null
          : count === 1
            ? 'También se borrará 1 relación de otra entidad que apunta a ella.'
            : `También se borrarán ${count} relaciones de otras entidades que apuntan a ella.`;
      this.pendingDelete.set({ kind, id, label: `la entidad ${displayName(entity?.name ?? '')}`, detail });
      return;
    }
    const superclass = state?.superclasses.find((candidate) => candidate.id === id);
    const dependents =
      (state?.superclasses.filter((candidate) => candidate.parentId === id).length ?? 0)
      + (state?.entities.filter((entity) => entity.superclassId === id).length ?? 0);
    const parent = state?.superclasses.find((candidate) => candidate.id === superclass?.parentId);
    const detail =
      dependents === 0
        ? null
        : parent
          ? `Sus superclases hijas y sus entidades pasarán a heredar de ${displayName(parent.name)}.`
          : 'Sus superclases hijas y sus entidades quedarán sin superclase.';
    this.pendingDelete.set({ kind, id, label: `la superclase ${displayName(superclass?.name ?? '')}`, detail });
  }

  /** Borra el elemento pendiente y, si estaba elegido, deja de elegirlo. */
  protected confirmDelete(): void {
    const pending = this.pendingDelete();
    if (!pending) {
      return;
    }
    if (pending.kind === 'entity') {
      removeEntity(this.form(), pending.id);
    } else {
      removeSuperclass(this.form(), pending.id);
    }
    if (this.isSelected(pending.kind, pending.id)) {
      this.selected.set(null);
    }
    this.pendingDelete.set(null);
  }

  /** Cancela el borrado pendiente. */
  protected cancelDelete(): void {
    this.pendingDelete.set(null);
  }
}