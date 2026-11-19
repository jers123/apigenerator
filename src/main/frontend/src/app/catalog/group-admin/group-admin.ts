import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { CatalogApi } from '../catalog-api';
import { groupForm, GroupForm, STANDALONE_CATEGORIES, toGroupDefinition } from '../group-editor/group-form';
import { DependencyGroup } from '../catalog.models';
import { problemMessage } from '../../shared/problem-detail';
import { GroupEditor } from '../group-editor/group-editor';

/** Se está creando un grupo nuevo. */
interface CreatingGroup {
  /** Modo del panel. */
  readonly mode: 'create';
}

/** Se está editando un grupo existente. */
interface EditingGroup {
  /** Modo del panel. */
  readonly mode: 'edit';
  /** Id del grupo. */
  readonly id: string;
}

/** Qué muestra el panel de edición. */
type Editing = CreatingGroup | EditingGroup;

/**
 * Pantalla para administrar los grupos del catálogo que no son de base de
 * datos; los de base de datos se editan con su motor.
 */
@Component({
  imports: [MatButtonModule, MatProgressBarModule, GroupEditor],
  selector: 'app-group-admin',
  styleUrl: './group-admin.css',
  templateUrl: './group-admin.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GroupAdmin {
  /** Cliente de la API del catálogo. */
  private readonly catalogApi = inject(CatalogApi);

  /** Categorías que se pueden elegir en esta pantalla. */
  protected readonly categories = STANDALONE_CATEGORIES;

  /** Catálogo cargado desde la API. */
  protected readonly catalog = rxResource({ stream: () => this.catalogApi.getCatalog() });

  /** Grupos de la lista: todos menos los de base de datos. */
  protected readonly groups = computed<readonly DependencyGroup[]>(() =>
    this.catalog.hasValue() ? this.catalog.value().groups.filter((group) => group.category !== 'DATABASE') : [],
  );

  /** Qué se está editando, o `null` si el panel está vacío. */
  protected readonly editing = signal<Editing | null>(null);

  /** Si se edita un grupo existente; su id queda fijo. */
  protected readonly isEdit = computed(() => this.editing()?.mode === 'edit');

  /** Definición del grupo que se edita. */
  private readonly definition = rxResource({
    params: () => {
      const editing = this.editing();
      return editing?.mode === 'edit' ? editing.id : undefined;
    },
    stream: ({ params }) => this.catalogApi.getGroup(params),
  });

  /** Formulario del panel: uno nuevo, o el del grupo cuando llega su definición. */
  protected readonly form = computed<GroupForm | null>(() => {
    const editing = this.editing();
    if (!editing) {
      return null;
    }
    if (editing.mode === 'create') {
      return groupForm('SPRING');
    }
    return this.definition.hasValue() ? groupForm('SPRING', this.definition.value()) : null;
  });

  /** Si hay un guardado en curso. */
  protected readonly saving = signal(false);

  /** Mensaje del último guardado fallido. */
  protected readonly saveError = signal<string | null>(null);

  /** Aviso del último guardado exitoso. */
  protected readonly notice = signal<string | null>(null);

  /** Mensaje de error al cargar datos, o `null`. */
  protected readonly loadError = computed(() => {
    const error = this.catalog.error() ?? this.definition.error();
    return error ? problemMessage(error) : null;
  });

  /** Abre el panel con un grupo nuevo. */
  protected startCreate(): void {
    this.open({ mode: 'create' });
  }

  /**
   * Abre el panel con un grupo existente; si ya está abierto, no lo recarga.
   * @param id id del grupo
   */
  protected edit(id: string): void {
    if (!this.isEditing(id)) {
      this.open({ mode: 'edit', id });
    }
  }

  /** Cierra el panel sin guardar. */
  protected cancel(): void {
    this.open(null);
  }

  /**
   * Indica si un grupo es el que está abierto.
   * @param id id del grupo
   * @returns `true` si se está editando
   */
  protected isEditing(id: string): boolean {
    const editing = this.editing();
    return editing?.mode === 'edit' && editing.id === id;
  }

  /**
   * Valida y envía el formulario. Tras crear, el panel pasa a editar el
   * grupo guardado; tras editar, conserva lo escrito.
   * @param form formulario del panel
   */
  protected save(form: GroupForm): void {
    form.markAllAsTouched();
    if (form.invalid) {
      this.saveError.set('Revisa los campos marcados antes de guardar.');
      return;
    }
    const creating = !this.isEdit();
    const group = toGroupDefinition(form);
    this.saving.set(true);
    this.saveError.set(null);
    this.notice.set(null);
    (creating ? this.catalogApi.createGroup(group) : this.catalogApi.updateGroup(group)).subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.notice.set(`Grupo "${saved.name}" guardado.`);
        this.catalog.reload();
        if (creating) {
          this.editing.set({ mode: 'edit', id: saved.id });
        }
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.saveError.set(problemMessage(error));
      },
    });
  }

  /**
   * Cambia lo que muestra el panel y limpia los mensajes anteriores.
   * @param editing nuevo contenido del panel
   */
  private open(editing: Editing | null): void {
    this.editing.set(editing);
    this.saveError.set(null);
    this.notice.set(null);
  }
}