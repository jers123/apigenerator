import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { outputFromObservable, rxResource, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { map, startWith, switchMap } from 'rxjs';
import { applyProvider, chooseIssuerType, SecurityForm, setIssuerEnabled } from '../project-form';
import { IssuerType } from '../project.models';
import { DependencyDefinition } from '../../catalog/catalog.models';
import { IdentityProviderDefinition, SecurityMechanismDefinition, SecurityRole } from '../../security/security.models';
import { SecurityApi } from '../../security/security-api';
import { problemMessage } from '../../shared/problem-detail';

/** Puerto del servidor de autorización generado; igual que en el backend. */
const AUTHORIZATION_SERVER_PORT = 9000;

/** Marcador `{clave}` de la plantilla del emisor. */
const PLACEHOLDER = /\{([A-Za-z][A-Za-z0-9]*)\}/g;

/** Librerías de seguridad que el paso informa al asistente. */
export interface SecurityDependencies {
  /** Artefactos de los métodos elegidos, sin repetir. */
  readonly chosen: readonly DependencyDefinition[];
  /** Coordenadas `groupId:artifactId` de todas las librerías del catálogo de seguridad. */
  readonly artifacts: readonly string[];
}

/**
 * Coordenadas de un artefacto.
 * @param dependency artefacto
 * @returns `groupId:artifactId`
 */
function coordinatesOf(dependency: DependencyDefinition): string {
  return `${dependency.groupId}:${dependency.artifactId}`;
}

/**
 * Compara dos resultados, para no avisar al asistente si no cambian.
 * @param a resultado anterior
 * @param b resultado nuevo
 * @returns `true` si tienen los mismos artefactos en el mismo orden
 */
function sameDependencies(a: SecurityDependencies, b: SecurityDependencies): boolean {
  const chosenA = a.chosen.map(coordinatesOf).join('|');
  const chosenB = b.chosen.map(coordinatesOf).join('|');
  return chosenA === chosenB && a.artifacts.join('|') === b.artifacts.join('|');
}

/**
 * Indica si dos métodos no se pueden combinar; basta con que uno lo declare.
 * @param a un método
 * @param b el otro
 * @returns `true` si chocan
 */
function conflict(a: SecurityMechanismDefinition, b: SecurityMechanismDefinition): boolean {
  return a.conflictsWith.includes(b.id) || b.conflictsWith.includes(a.id);
}

/**
 * Paso "Seguridad" del asistente: autenticación de la API, inicio de sesión,
 * complementos y, si algún método lo necesita, el emisor de tokens.
 *
 * Muestra todas las opciones; las que no se pueden elegir quedan
 * deshabilitadas con su motivo. Informa al asistente qué librerías agrega.
 */
@Component({
  imports: [ReactiveFormsModule, MatCheckboxModule, MatFormFieldModule, MatInputModule, MatProgressBarModule, MatRadioModule, MatSelectModule],
  selector: 'app-security-step',
  styleUrl: './security-step.css',
  templateUrl: './security-step.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SecurityStep {
  /** Formulario del paso. */
  readonly form = input.required<SecurityForm>();

  /** Modo reactivo elegido en el paso "Proyecto". */
  readonly reactive = input.required<boolean>();

  /** artifactId del proyecto, para nombrar el segundo proyecto. */
  readonly artifactId = input('');

  /** Cliente de la API de seguridad. */
  private readonly api = inject(SecurityApi);

  /** Puerto del servidor de autorización generado. */
  protected readonly authorizationServerPort = AUTHORIZATION_SERVER_PORT;

  /** Métodos de seguridad del catálogo. */
  protected readonly mechanisms = rxResource({ stream: () => this.api.mechanisms() });

  /** Proveedores de identidad del catálogo. */
  protected readonly providers = rxResource({ stream: () => this.api.providers() });

  /** Métodos, o vacía mientras carga. */
  private readonly mechanismList = computed<readonly SecurityMechanismDefinition[]>(() =>
    this.mechanisms.hasValue() ? this.mechanisms.value() : [],
  );

  /** Proveedores, o vacía mientras carga. */
  protected readonly providerList = computed<readonly IdentityProviderDefinition[]>(() =>
    this.providers.hasValue() ? this.providers.value() : [],
  );

  /** Métodos de autenticación de la API. */
  protected readonly apiOptions = computed(() => this.withRole('API_AUTHENTICATION'));

  /** Métodos de inicio de sesión. */
  protected readonly loginOptions = computed(() => this.withRole('BROWSER_LOGIN'));

  /** Complementos. */
  protected readonly addOnOptions = computed(() => this.withRole('ADD_ON'));

  /** Valor completo del formulario; se actualiza con cada cambio. */
  private readonly state = toSignal(
    toObservable(this.form).pipe(
      switchMap((form) => form.valueChanges.pipe(startWith(null), map(() => form.getRawValue()))),
    ),
  );

  /** Id de la autenticación de la API, o `null`. */
  protected readonly apiId = computed(() => this.state()?.apiAuthenticationId ?? null);

  /** Id del inicio de sesión, o `null`. */
  protected readonly loginId = computed(() => this.state()?.browserLoginId ?? null);

  /** Ids de los complementos elegidos. */
  protected readonly addOnIds = computed<readonly string[]>(() => this.state()?.addOnIds ?? []);

  /** Tipo de emisor elegido, o `null`. */
  protected readonly issuerType = computed(() => this.state()?.issuer.type ?? null);

  /** Id del proveedor elegido, o vacío. */
  protected readonly providerId = computed(() => this.state()?.issuer.providerId ?? '');

  /** Métodos elegidos: API, login y complementos. */
  private readonly chosen = computed<readonly SecurityMechanismDefinition[]>(() =>
    [this.apiId(), this.loginId(), ...this.addOnIds()]
      .map((id) => (id ? this.find(id) : undefined))
      .filter((mechanism): mechanism is SecurityMechanismDefinition => mechanism !== undefined),
  );

  /** Si algún método elegido necesita emisor de tokens. */
  protected readonly needsIssuer = computed(() => this.chosen().some((mechanism) => mechanism.requiresIssuer));

  /** Nombres de los métodos que necesitan emisor. */
  protected readonly issuerUsers = computed(() =>
    this.chosen().filter((mechanism) => mechanism.requiresIssuer).map((mechanism) => mechanism.name).join(', '),
  );

  /** Servidor de autorización del catálogo, si existe. */
  private readonly authorizationServer = computed(() =>
    this.mechanismList().find((mechanism) => mechanism.role === 'AUTHORIZATION_SERVER'),
  );

  /** Por qué no se puede usar el servidor propio, o `null`. */
  protected readonly ownServerReason = computed(() => {
    const server = this.authorizationServer();
    if (!server) {
      return 'No hay servidor de autorización en el catálogo.';
    }
    return this.reactive() && !server.reactiveSupported ? 'No funciona en proyectos reactivos.' : null;
  });

  /** Proveedor elegido, si existe. */
  protected readonly selectedProvider = computed(() =>
    this.providerList().find((provider) => provider.id === this.providerId()),
  );

  /** URL del emisor con los datos escritos; los que faltan quedan como marcador. */
  protected readonly issuerPreview = computed(() => {
    const template = this.selectedProvider()?.issuerUriTemplate;
    if (!template) {
      return null;
    }
    const values: Readonly<Record<string, string>> = this.state()?.issuer.values ?? {};
    return template.replace(PLACEHOLDER, (match: string, key: string) => {
      const value = (values[key] as string | undefined)?.trim();
      return value ? value : match;
    });
  });

  /** Aviso de lo que se quitó al pasar a reactivo, o `null`. */
  protected readonly notice = signal<string | null>(null);

  /** Mensaje si los catálogos no se pudieron cargar. */
  protected readonly loadError = computed(() => {
    const error = this.mechanisms.error() ?? this.providers.error();
    return error ? problemMessage(error) : null;
  });

  /** Librerías de los métodos elegidos y de todo el catálogo de seguridad. */
  private readonly dependencies = computed<SecurityDependencies>(
    () => {
      const unique = new Map<string, DependencyDefinition>();
      for (const mechanism of this.chosen()) {
        for (const dependency of mechanism.dependencies) {
          if (!unique.has(coordinatesOf(dependency))) {
            unique.set(coordinatesOf(dependency), dependency);
          }
        }
      }
      const artifacts = [
        ...new Set(this.mechanismList().flatMap((mechanism) => mechanism.dependencies.map(coordinatesOf))),
      ].sort();
      return { chosen: [...unique.values()], artifacts };
    },
    { equal: sameDependencies },
  );

  /** Avisa al asistente cuando cambian las librerías de seguridad. */
  readonly dependenciesChange = outputFromObservable(toObservable(this.dependencies));

  /** Al pasar a reactivo, quita lo que no funciona en ese modo. */
  constructor() {
    effect(() => {
      const reactive = this.reactive();
      const loaded = this.mechanismList().length > 0;
      if (reactive && loaded) {
        untracked(() => this.dropUnsupported());
      }
    });
  }

  /**
   * Por qué no se puede elegir un método, o `null` si se puede.
   * @param mechanism método del catálogo
   * @returns el motivo
   */
  protected reasonFor(mechanism: SecurityMechanismDefinition): string | null {
    if (this.reactive() && !mechanism.reactiveSupported) {
      return 'No funciona en proyectos reactivos.';
    }
    if (mechanism.role === 'ADD_ON' && this.apiId() === null) {
      return 'Necesita autenticación de la API.';
    }
    const clash = this.chosen().find(
      (other) =>
        other.id !== mechanism.id &&
        (other.role !== mechanism.role || mechanism.role === 'ADD_ON') &&
        conflict(other, mechanism),
    );
    return clash ? `No se combina con ${clash.name}.` : null;
  }

  /**
   * Por qué no sirve un proveedor para lo elegido, o `null` si sirve.
   * @param provider proveedor del catálogo
   * @returns el motivo
   */
  protected providerReason(provider: IdentityProviderDefinition): string | null {
    const apiId = this.apiId();
    const loginId = this.loginId();
    const api = apiId ? this.find(apiId) : undefined;
    const login = loginId ? this.find(loginId) : undefined;
    if (api?.requiresIssuer && !provider.supportsResourceServer) {
      return 'No emite tokens que la API pueda validar.';
    }
    if (login?.requiresIssuer && !provider.supportsLogin) {
      return 'No sirve para el inicio de sesión.';
    }
    return null;
  }

  /**
   * Indica si un complemento está elegido.
   * @param id id del complemento
   * @returns `true` si está marcado
   */
  protected isAddOnChecked(id: string): boolean {
    return this.addOnIds().includes(id);
  }

  /**
   * Elige la autenticación de la API; sin ella se quitan los complementos.
   * @param value id del método, o vacío para "Ninguna"
   */
  protected chooseApi(value: string): void {
    const controls = this.form().controls;
    const id = value === '' ? null : value;
    controls.apiAuthenticationId.setValue(id);
    if (id === null) {
      controls.addOnIds.setValue([]);
    }
    this.notice.set(null);
    this.syncIssuer();
  }

  /**
   * Elige el inicio de sesión.
   * @param value id del método, o vacío para ninguno
   */
  protected chooseLogin(value: string): void {
    this.form().controls.browserLoginId.setValue(value === '' ? null : value);
    this.notice.set(null);
    this.syncIssuer();
  }

  /**
   * Marca o desmarca un complemento.
   * @param id id del complemento
   * @param checked nuevo estado
   */
  protected toggleAddOn(id: string, checked: boolean): void {
    const current = this.form().controls.addOnIds.value.filter((other) => other !== id);
    this.form().controls.addOnIds.setValue(checked ? [...current, id] : current);
    this.syncIssuer();
  }

  /**
   * Elige el tipo de emisor.
   * @param value `OWN_SERVER`, `PROVIDER` o vacío
   */
  protected chooseIssuer(value: string): void {
    chooseIssuerType(this.form().controls.issuer, value === '' ? null : (value as IssuerType));
  }

  /**
   * Elige el proveedor y crea los campos de sus datos.
   * @param id id del proveedor
   */
  protected chooseProvider(id: string): void {
    applyProvider(this.form().controls.issuer, this.providerList().find((provider) => provider.id === id) ?? null);
  }

  /**
   * Campo del dato de un parámetro.
   * @param key clave del parámetro
   * @returns el control, o `undefined` si aún no existe
   */
  protected valueControl(key: string): FormControl<string> | undefined {
    return this.form().controls.issuer.controls.values.controls[key] as FormControl<string> | undefined;
  }

  /**
   * Busca un método por su id.
   * @param id id del método
   * @returns el método, o `undefined`
   */
  private find(id: string): SecurityMechanismDefinition | undefined {
    return this.mechanismList().find((mechanism) => mechanism.id === id);
  }

  /**
   * Métodos de un tipo, en el orden del catálogo.
   * @param role tipo
   * @returns métodos del tipo
   */
  private withRole(role: SecurityRole): readonly SecurityMechanismDefinition[] {
    return this.mechanismList().filter((mechanism) => mechanism.role === role);
  }

  /**
   * Habilita el emisor solo si hace falta y descarta lo que dejó de servir:
   * el servidor propio en reactivo o un proveedor que no sirve para lo elegido.
   */
  private syncIssuer(): void {
    const issuer = this.form().controls.issuer;
    const needed = this.needsIssuer();
    setIssuerEnabled(issuer, needed);
    if (!needed) {
      return;
    }
    if (issuer.controls.type.value === 'OWN_SERVER' && this.ownServerReason() !== null) {
      chooseIssuerType(issuer, null);
    }
    const provider = this.selectedProvider();
    if (provider && this.providerReason(provider) !== null) {
      applyProvider(issuer, null);
    }
  }

  /** Quita lo que no funciona en reactivo y avisa qué se quitó. */
  private dropUnsupported(): void {
    const controls = this.form().controls;
    const removed: string[] = [];
    const keep = (id: string | null): string | null => {
      const mechanism = id ? this.find(id) : undefined;
      if (mechanism && !mechanism.reactiveSupported) {
        removed.push(mechanism.name);
        return null;
      }
      return id;
    };
    controls.apiAuthenticationId.setValue(keep(controls.apiAuthenticationId.value));
    controls.browserLoginId.setValue(keep(controls.browserLoginId.value));
    controls.addOnIds.setValue(
      controls.apiAuthenticationId.value ? controls.addOnIds.value.filter((id) => keep(id) !== null) : [],
    );
    if (controls.issuer.enabled && controls.issuer.controls.type.value === 'OWN_SERVER' && this.ownServerReason() !== null) {
      removed.push('Servidor de autorización propio');
    }
    this.syncIssuer();
    if (removed.length > 0) {
      this.notice.set(`Se quitó porque no funciona en proyectos reactivos: ${removed.join(', ')}.`);
    }
  }
}