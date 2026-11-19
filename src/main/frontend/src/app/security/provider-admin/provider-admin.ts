import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { addMissingParameters, issuerPreview, parameterForm, providerForm, ProviderForm, TemplateMismatch, toProviderDefinition } from './provider-form';
import { HOSTING_LABELS, IdentityProviderDefinition, PROVIDER_HOSTINGS } from '../security.models';
import { SecurityApi } from '../security-api';
import { problemMessage } from '../../shared/problem-detail';

/** Se está creando un proveedor nuevo. */
interface CreatingProvider {
  /** Modo del panel. */
  readonly mode: 'create';
}

/** Se está editando un proveedor existente. */
interface EditingProvider {
  /** Modo del panel. */
  readonly mode: 'edit';
  /** Proveedor tal como llegó en la lista. */
  readonly provider: IdentityProviderDefinition;
}

/** Qué muestra el panel de edición. */
type Editing = CreatingProvider | EditingProvider;

/**
 * Pestaña para administrar los proveedores de identidad externos: su URL de
 * emisor, los datos que piden y para qué sirven.
 */
@Component({
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  selector: 'app-provider-admin',
  styleUrls: ['../../shared/admin-layout.css', './provider-admin.css'],
  templateUrl: './provider-admin.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderAdmin {
  /** Cliente de la API de seguridad. */
  private readonly api = inject(SecurityApi);

  /** Alojamientos que se ofrecen. */
  protected readonly hostings = PROVIDER_HOSTINGS;

  /** Nombre legible de cada alojamiento. */
  protected readonly hostingLabels = HOSTING_LABELS;

  /** Ayuda de la plantilla; va en código porque las llaves no se pueden escribir en la plantilla HTML. */
  protected readonly templateHint = 'Usa {clave} para cada dato, por ejemplo https://{domain}/';

  /** Proveedores cargados desde la API. */
  protected readonly providers = rxResource({ stream: () => this.api.providers() });

  /** Proveedores para la lista, o vacía mientras carga o si falló. */
  protected readonly providerList = computed<readonly IdentityProviderDefinition[]>(() =>
    this.providers.hasValue() ? this.providers.value() : [],
  );

  /** Qué se está editando, o `null` si el panel está vacío. */
  protected readonly editing = signal<Editing | null>(null);

  /** Si se edita un proveedor existente; su id queda fijo. */
  protected readonly isEdit = computed(() => this.editing()?.mode === 'edit');

  /** Formulario del panel; se crea de nuevo solo al cambiar lo que se edita. */
  protected readonly form = computed<ProviderForm | null>(() => {
    const editing = this.editing();
    if (!editing) {
      return null;
    }
    return editing.mode === 'create' ? providerForm() : providerForm(editing.provider);
  });

  /** Si hay un guardado en curso. */
  protected readonly saving = signal(false);

  /** Mensaje del último guardado fallido. */
  protected readonly saveError = signal<string | null>(null);

  /** Aviso del último guardado exitoso. */
  protected readonly notice = signal<string | null>(null);

  /** Mensaje de error al cargar la lista, o `null`. */
  protected readonly loadError = computed(() => {
    const error = this.providers.error();
    return error ? problemMessage(error) : null;
  });

  /**
   * Para qué sirve un proveedor, en pocas palabras.
   * @param provider proveedor de la lista
   * @returns "API · Login", "API" o "Login"
   */
  protected usesOf(provider: IdentityProviderDefinition): string {
    return [provider.supportsResourceServer ? 'API' : null, provider.supportsLogin ? 'Login' : null]
      .filter((use) => use !== null)
      .join(' · ');
  }

  /**
   * Diferencias entre la plantilla y los parámetros.
   * @param form formulario del proveedor
   * @returns las diferencias, o `null` si coinciden
   */
  protected mismatchOf(form: ProviderForm): TemplateMismatch | null {
    return form.getError('templateMismatch') as TemplateMismatch | null;
  }

  /**
   * URL del emisor armada con los ejemplos.
   * @param form formulario del proveedor
   * @returns URL de ejemplo, o `null` si no hay plantilla
   */
  protected preview(form: ProviderForm): string | null {
    return issuerPreview(form);
  }

  /**
   * Crea los parámetros que pide la plantilla y aún no existen.
   * @param form formulario del proveedor
   */
  protected addMissing(form: ProviderForm): void {
    addMissingParameters(form);
  }

  /**
   * Agrega un parámetro en blanco.
   * @param form formulario del proveedor
   */
  protected addParameter(form: ProviderForm): void {
    form.controls.parameters.push(parameterForm());
  }

  /**
   * Quita un parámetro.
   * @param form formulario del proveedor
   * @param index posición del parámetro
   */
  protected removeParameter(form: ProviderForm, index: number): void {
    form.controls.parameters.removeAt(index);
  }

  /** Abre el panel con un proveedor nuevo. */
  protected startCreate(): void {
    this.open({ mode: 'create' });
  }

  /**
   * Abre el panel con un proveedor existente; si ya está abierto, no lo recarga.
   * @param provider proveedor de la lista
   */
  protected edit(provider: IdentityProviderDefinition): void {
    if (!this.isEditing(provider.id)) {
      this.open({ mode: 'edit', provider });
    }
  }

  /** Cierra el panel sin guardar. */
  protected cancel(): void {
    this.open(null);
  }

  /**
   * Indica si un proveedor es el que está abierto.
   * @param id id del proveedor
   * @returns `true` si se está editando
   */
  protected isEditing(id: string): boolean {
    const editing = this.editing();
    return editing?.mode === 'edit' && editing.provider.id === id;
  }

  /**
   * Valida y envía el formulario. Tras crear, el panel pasa a editar el
   * proveedor guardado; tras editar, conserva lo escrito.
   * @param form formulario del panel
   */
  protected save(form: ProviderForm): void {
    form.markAllAsTouched();
    if (form.invalid) {
      this.saveError.set('Revisa los campos marcados antes de guardar.');
      return;
    }
    const creating = !this.isEdit();
    const definition = toProviderDefinition(form);
    this.saving.set(true);
    this.saveError.set(null);
    this.notice.set(null);
    (creating ? this.api.createProvider(definition) : this.api.updateProvider(definition)).subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.notice.set(`Proveedor "${saved.name}" guardado.`);
        this.providers.reload();
        if (creating) {
          this.editing.set({ mode: 'edit', provider: saved });
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