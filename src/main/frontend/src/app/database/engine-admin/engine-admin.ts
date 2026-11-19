import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { engineForm, EngineForm, toEngineRequest } from './engine-form';
import { DATABASE_KINDS, DatabaseEngine } from '../database.models';
import { DatabaseApi } from '../database-api';
import { GroupEditor } from '../../catalog/group-editor/group-editor';
import { DependencyCategory } from '../../catalog/catalog.models';
import { CatalogApi } from '../../catalog/catalog-api';
import { problemMessage } from '../../shared/problem-detail';

/** Se está creando un motor nuevo. */
interface CreatingEngine {
  /** Modo del panel. */
  readonly mode: 'create';
}

/** Se está editando un motor existente. */
interface EditingEngine {
  /** Modo del panel. */
  readonly mode: 'edit';
  /** Motor tal como llegó en la lista. */
  readonly engine: DatabaseEngine;
}

/** Qué muestra el panel de edición. */
type Editing = CreatingEngine | EditingEngine;

/**
 * Pantalla para administrar los motores de base de datos. Cada motor se
 * edita junto con el grupo del catálogo que aporta su driver.
 */
@Component({
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatInputModule, MatProgressBarModule, MatSelectModule, GroupEditor],
  selector: 'app-engine-admin',
  styleUrl: '../../shared/admin-layout.css',
  //styleUrl: './engine-admin.css',
  templateUrl: './engine-admin.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EngineAdmin {
  /** Cliente de la API de motores. */
  private readonly databaseApi = inject(DatabaseApi);

  /** Cliente de la API del catálogo, para leer el grupo de driver. */
  private readonly catalogApi = inject(CatalogApi);

  /** El grupo de driver solo puede ser de base de datos. */
  protected readonly databaseCategories: readonly DependencyCategory[] = ['DATABASE'];

  /** Tipos de base que se ofrecen; los no soportados aparecen deshabilitados. */
  protected readonly kinds = DATABASE_KINDS;

  /** Ayuda del campo de plantilla; va en código porque las llaves no se pueden escribir en la plantilla HTML. */
  protected readonly templateHint = 'Usa {host}, {port} y {database}';

  /** Motores cargados desde la API. */
  protected readonly engines = rxResource({ stream: () => this.databaseApi.list() });

  /** Motores para la lista, o vacía mientras carga o si falló. */
  protected readonly engineList = computed<readonly DatabaseEngine[]>(() =>
    this.engines.hasValue() ? this.engines.value() : [],
  );

  /** Qué se está editando, o `null` si el panel está vacío. */
  protected readonly editing = signal<Editing | null>(null);

  /** Si se edita un motor existente; su id y el de su grupo quedan fijos. */
  protected readonly isEdit = computed(() => this.editing()?.mode === 'edit');

  /** Grupo de driver del motor que se edita. */
  private readonly driverGroup = rxResource({
    params: () => {
      const editing = this.editing();
      return editing?.mode === 'edit' ? editing.engine.driverGroupId : undefined;
    },
    stream: ({ params }) => this.catalogApi.getGroup(params),
  });

  /** Formulario del panel: uno nuevo, o el del motor cuando llega su grupo. */
  protected readonly form = computed<EngineForm | null>(() => {
    const editing = this.editing();
    if (!editing) {
      return null;
    }
    if (editing.mode === 'create') {
      return engineForm();
    }
    return this.driverGroup.hasValue() ? engineForm(editing.engine, this.driverGroup.value()) : null;
  });

  /** Si hay un guardado en curso. */
  protected readonly saving = signal(false);

  /** Mensaje del último guardado fallido. */
  protected readonly saveError = signal<string | null>(null);

  /** Aviso del último guardado exitoso. */
  protected readonly notice = signal<string | null>(null);

  /** Mensaje de error al cargar datos, o `null`. */
  protected readonly loadError = computed(() => {
    const error = this.engines.error() ?? this.driverGroup.error();
    return error ? problemMessage(error) : null;
  });

  /** Abre el panel con un motor nuevo. */
  protected startCreate(): void {
    this.open({ mode: 'create' });
  }

  /**
   * Abre el panel con un motor existente; si ya está abierto, no lo recarga.
   * @param engine motor de la lista
   */
  protected edit(engine: DatabaseEngine): void {
    if (!this.isEditing(engine.id)) {
      this.open({ mode: 'edit', engine });
    }
  }

  /** Cierra el panel sin guardar. */
  protected cancel(): void {
    this.open(null);
  }

  /**
   * Indica si un motor es el que está abierto.
   * @param id id del motor
   * @returns `true` si se está editando
   */
  protected isEditing(id: string): boolean {
    const editing = this.editing();
    return editing?.mode === 'edit' && editing.engine.id === id;
  }

  /**
   * Valida y envía el formulario. Tras crear, el panel pasa a editar el
   * motor guardado; tras editar, conserva lo escrito.
   * @param form formulario del panel
   */
  protected save(form: EngineForm): void {
    form.markAllAsTouched();
    if (form.invalid) {
      this.saveError.set('Revisa los campos marcados antes de guardar.');
      return;
    }
    const creating = !this.isEdit();
    const request = toEngineRequest(form);
    this.saving.set(true);
    this.saveError.set(null);
    this.notice.set(null);
    (creating ? this.databaseApi.create(request) : this.databaseApi.update(request)).subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.notice.set(`Motor "${saved.label}" guardado.`);
        this.engines.reload();
        if (creating) {
          this.editing.set({ mode: 'edit', engine: saved });
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