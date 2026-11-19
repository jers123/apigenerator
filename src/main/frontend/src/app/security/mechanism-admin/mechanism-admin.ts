import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { mayRequireIssuer, mechanismForm, MechanismForm, toMechanismDefinition } from './mechanism-form';
import { SECURITY_ROLE_LABELS, SECURITY_ROLES, SecurityMechanismDefinition } from '../security.models';
import { SecurityApi } from '../security-api';
import { DependencyRows } from '../../catalog/dependency-rows/dependency-rows';
import { problemMessage } from '../../shared/problem-detail';

/** Se está creando un método nuevo. */
interface CreatingMechanism {
  /** Modo del panel. */
  readonly mode: 'create';
}

/** Se está editando un método existente. */
interface EditingMechanism {
  /** Modo del panel. */
  readonly mode: 'edit';
  /** Método tal como llegó en la lista. */
  readonly mechanism: SecurityMechanismDefinition;
}

/** Qué muestra el panel de edición. */
type Editing = CreatingMechanism | EditingMechanism;

/**
 * Pestaña para administrar los métodos de seguridad: su tipo, sus reglas de
 * combinación y las dependencias que agregan.
 */
@Component({
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatInputModule, MatSelectModule, DependencyRows],
  selector: 'app-mechanism-admin',
  //styleUrl: './mechanism-admin.css',
  styleUrl: '../../shared/admin-layout.css',
  templateUrl: './mechanism-admin.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MechanismAdmin {
  /** Cliente de la API de seguridad. */
  private readonly api = inject(SecurityApi);

  /** Tipos que se ofrecen. */
  protected readonly roles = SECURITY_ROLES;

  /** Nombre legible de cada tipo. */
  protected readonly roleLabels = SECURITY_ROLE_LABELS;

  /** Métodos cargados desde la API. */
  protected readonly mechanisms = rxResource({ stream: () => this.api.mechanisms() });

  /** Métodos para la lista, o vacía mientras carga o si falló. */
  protected readonly mechanismList = computed<readonly SecurityMechanismDefinition[]>(() =>
    this.mechanisms.hasValue() ? this.mechanisms.value() : [],
  );

  /** Qué se está editando, o `null` si el panel está vacío. */
  protected readonly editing = signal<Editing | null>(null);

  /** Si se edita un método existente; su id queda fijo. */
  protected readonly isEdit = computed(() => this.editing()?.mode === 'edit');

  /** Formulario del panel; se crea de nuevo solo al cambiar lo que se edita. */
  protected readonly form = computed<MechanismForm | null>(() => {
    const editing = this.editing();
    if (!editing) {
      return null;
    }
    return editing.mode === 'create' ? mechanismForm() : mechanismForm(editing.mechanism);
  });

  /** Métodos con los que puede chocar el que se edita: todos menos él mismo. */
  protected readonly conflictOptions = computed(() => {
    const editing = this.editing();
    const own = editing?.mode === 'edit' ? editing.mechanism.id : null;
    return this.mechanismList().filter((mechanism) => mechanism.id !== own);
  });

  /** Si hay un guardado en curso. */
  protected readonly saving = signal(false);

  /** Mensaje del último guardado fallido. */
  protected readonly saveError = signal<string | null>(null);

  /** Aviso del último guardado exitoso. */
  protected readonly notice = signal<string | null>(null);

  /** Mensaje de error al cargar la lista, o `null`. */
  protected readonly loadError = computed(() => {
    const error = this.mechanisms.error();
    return error ? problemMessage(error) : null;
  });

  /** Abre el panel con un método nuevo. */
  protected startCreate(): void {
    this.open({ mode: 'create' });
  }

  /**
   * Abre el panel con un método existente; si ya está abierto, no lo recarga.
   * @param mechanism método de la lista
   */
  protected edit(mechanism: SecurityMechanismDefinition): void {
    if (!this.isEditing(mechanism.id)) {
      this.open({ mode: 'edit', mechanism });
    }
  }

  /** Cierra el panel sin guardar. */
  protected cancel(): void {
    this.open(null);
  }

  /**
   * Indica si un método es el que está abierto.
   * @param id id del método
   * @returns `true` si se está editando
   */
  protected isEditing(id: string): boolean {
    const editing = this.editing();
    return editing?.mode === 'edit' && editing.mechanism.id === id;
  }

  /**
   * Indica si se muestra la marca "Necesita emisor": cuando el tipo lo
   * admite o, para poder quitarla, cuando ya está marcada.
   * @param form formulario del método
   * @returns `true` si se muestra
   */
  protected showsIssuer(form: MechanismForm): boolean {
    return mayRequireIssuer(form.controls.role.value) || form.controls.requiresIssuer.value;
  }

  /**
   * Valida y envía el formulario. Tras crear, el panel pasa a editar el
   * método guardado; tras editar, conserva lo escrito.
   * @param form formulario del panel
   */
  protected save(form: MechanismForm): void {
    form.markAllAsTouched();
    if (form.invalid) {
      this.saveError.set('Revisa los campos marcados antes de guardar.');
      return;
    }
    const creating = !this.isEdit();
    const definition = toMechanismDefinition(form);
    this.saving.set(true);
    this.saveError.set(null);
    this.notice.set(null);
    (creating ? this.api.createMechanism(definition) : this.api.updateMechanism(definition)).subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.notice.set(`Método "${saved.name}" guardado.`);
        this.mechanisms.reload();
        if (creating) {
          this.editing.set({ mode: 'edit', mechanism: saved });
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