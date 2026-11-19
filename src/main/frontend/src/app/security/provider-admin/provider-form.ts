import { FormArray, FormControl, FormGroup, ValidatorFn, Validators } from "@angular/forms";
import { IdentityProviderDefinition, ProviderHosting, ProviderParameter } from "../security.models";
import { notBlank, SLUG_PATTERN } from "../../catalog/group-editor/group-form";

/** Clave de un parámetro: letras y números, empezando por letra; igual que en el backend. */
export const PARAMETER_KEY_PATTERN = /^[A-Za-z][A-Za-z0-9]*$/;

/** Marcador `{clave}` dentro de la plantilla del emisor. */
const PLACEHOLDER = /\{([A-Za-z][A-Za-z0-9]*)\}/g;

/** Formulario de un parámetro del proveedor. */
export type ParameterForm = FormGroup<{
  /** Nombre del marcador, sin llaves. */
  key: FormControl<string>;
  /** Texto que ve el usuario. */
  label: FormControl<string>;
  /** Valor de ejemplo. */
  example: FormControl<string>;
}>;

/** Formulario de un proveedor de identidad. */
export type ProviderForm = FormGroup<{
  /** Identificador estable, con formato slug. */
  id: FormControl<string>;
  /** Nombre visible. */
  name: FormControl<string>;
  /** Explicación breve. */
  description: FormControl<string>;
  /** Dónde funciona. */
  hosting: FormControl<ProviderHosting>;
  /** URL del emisor con marcadores; vacía si Spring Boot ya conoce al proveedor. */
  issuerUriTemplate: FormControl<string>;
  /** Datos que se piden para completar la plantilla. */
  parameters: FormArray<ParameterForm>;
  /** Si la API puede validar sus tokens. */
  supportsResourceServer: FormControl<boolean>;
  /** Si sirve para el inicio de sesión. */
  supportsLogin: FormControl<boolean>;
  /** Documentación oficial; vacía si no tiene. */
  documentationUrl: FormControl<string>;
}>;

/** Diferencias entre la plantilla y los parámetros declarados. */
export interface TemplateMismatch {
  /** Marcadores de la plantilla sin parámetro. */
  readonly undeclared: readonly string[];
  /** Parámetros que la plantilla no usa. */
  readonly unused: readonly string[];
}

/**
 * Marcadores de una plantilla, sin repetir y en orden de aparición.
 * @param template plantilla del emisor
 * @returns claves de los marcadores
 */
export function placeholdersOf(template: string): string[] {
  return [...new Set([...template.matchAll(PLACEHOLDER)].map((match) => match[1]))];
}

/**
 * Exige que la plantilla, si existe, empiece por `https://` o por un marcador.
 * @param control campo de la plantilla
 * @returns `{ insecure: true }` si no cumple
 */
const secureTemplate: ValidatorFn = (control) => {
  const value = (control.value as string).trim();
  return value === '' || value.startsWith('https://') || value.startsWith('{') ? null : { insecure: true };
};

/**
 * Exige que la documentación, si existe, sea `https://`.
 * @param control campo de la documentación
 * @returns `{ https: true }` si no cumple
 */
const httpsOrBlank: ValidatorFn = (control) => {
  const value = (control.value as string).trim();
  return value === '' || value.startsWith('https://') ? null : { https: true };
};

/**
 * Exige que los marcadores de la plantilla y los parámetros coincidan uno a uno.
 * @param control formulario del proveedor
 * @returns `{ templateMismatch: TemplateMismatch }` si hay diferencias
 */
const templateMatchesParameters: ValidatorFn = (control) => {
  const value = (control as ProviderForm).getRawValue();
  const template = value.issuerUriTemplate.trim();
  if (template === '') {
    return null;
  }
  const keys = value.parameters.map((parameter) => parameter.key.trim()).filter((key) => key !== '');
  const used = placeholdersOf(template);
  const undeclared = used.filter((key) => !keys.includes(key));
  const unused = keys.filter((key) => !used.includes(key));
  return undeclared.length > 0 || unused.length > 0 ? { templateMismatch: { undeclared, unused } } : null;
};

/**
 * Exige que el proveedor sirva para la API, para el login o para ambos.
 * @param control formulario del proveedor
 * @returns `{ noUse: true }` si no sirve para nada
 */
const servesSomething: ValidatorFn = (control) => {
  const { supportsResourceServer, supportsLogin } = (control as ProviderForm).getRawValue();
  return supportsResourceServer || supportsLogin ? null : { noUse: true };
};

/**
 * Exige plantilla cuando el proveedor sirve para la API, que valida los tokens contra ella.
 * @param control formulario del proveedor
 * @returns `{ templateRequired: true }` si falta
 */
const apiNeedsTemplate: ValidatorFn = (control) => {
  const { supportsResourceServer, issuerUriTemplate } = (control as ProviderForm).getRawValue();
  return supportsResourceServer && issuerUriTemplate.trim() === '' ? { templateRequired: true } : null;
};

/**
 * Crea el formulario de un parámetro.
 * @param parameter parámetro existente; sin él, uno en blanco
 * @returns formulario con las reglas del parámetro
 */
export function parameterForm(parameter?: ProviderParameter): ParameterForm {
  return new FormGroup({
    key: new FormControl(parameter?.key ?? '', {
      nonNullable: true,
      validators: [notBlank, Validators.pattern(PARAMETER_KEY_PATTERN)],
    }),
    label: new FormControl(parameter?.label ?? '', { nonNullable: true, validators: notBlank }),
    example: new FormControl(parameter?.example ?? '', { nonNullable: true }),
  });
}

/**
 * Crea el formulario de un proveedor.
 * @param definition proveedor existente; sin él, uno en la nube para la API y el login
 * @returns formulario con las reglas del proveedor
 */
export function providerForm(definition?: IdentityProviderDefinition): ProviderForm {
  return new FormGroup(
    {
      id: new FormControl(definition?.id ?? '', {
        nonNullable: true,
        validators: [notBlank, Validators.pattern(SLUG_PATTERN)],
      }),
      name: new FormControl(definition?.name ?? '', { nonNullable: true, validators: notBlank }),
      description: new FormControl(definition?.description ?? '', { nonNullable: true }),
      hosting: new FormControl<ProviderHosting>(definition?.hosting ?? 'CLOUD', { nonNullable: true }),
      issuerUriTemplate: new FormControl(definition?.issuerUriTemplate ?? '', {
        nonNullable: true,
        validators: secureTemplate,
      }),
      parameters: new FormArray((definition?.parameters ?? []).map((parameter) => parameterForm(parameter))),
      supportsResourceServer: new FormControl(definition?.supportsResourceServer ?? true, { nonNullable: true }),
      supportsLogin: new FormControl(definition?.supportsLogin ?? true, { nonNullable: true }),
      documentationUrl: new FormControl(definition?.documentationUrl ?? '', {
        nonNullable: true,
        validators: httpsOrBlank,
      }),
    },
    { validators: [templateMatchesParameters, servesSomething, apiNeedsTemplate] },
  );
}

/**
 * Agrega un parámetro por cada marcador de la plantilla que aún no tiene;
 * la etiqueta empieza siendo la clave.
 * @param form formulario del proveedor
 */
export function addMissingParameters(form: ProviderForm): void {
  const mismatch = form.getError('templateMismatch') as TemplateMismatch | null;
  for (const key of mismatch?.undeclared ?? []) {
    form.controls.parameters.push(parameterForm({ key, label: key, example: '' }));
  }
}

/**
 * URL del emisor armada con los ejemplos de los parámetros; los marcadores
 * sin ejemplo quedan como están.
 * @param form formulario del proveedor
 * @returns URL de ejemplo, o `null` si no hay plantilla
 */
export function issuerPreview(form: ProviderForm): string | null {
  const value = form.getRawValue();
  const template = value.issuerUriTemplate.trim();
  if (template === '') {
    return null;
  }
  const examples = new Map(value.parameters.map((parameter) => [parameter.key.trim(), parameter.example.trim()]));
  return template.replace(PLACEHOLDER, (match: string, key: string) => {
    const example = examples.get(key);
    return example ? example : match;
  });
}

/**
 * Convierte el formulario en el cuerpo que espera la API: recorta textos y
 * deja `null` la plantilla y la documentación vacías.
 * @param form formulario del proveedor
 * @returns definición lista para `POST` o `PUT`
 */
export function toProviderDefinition(form: ProviderForm): IdentityProviderDefinition {
  const value = form.getRawValue();
  const template = value.issuerUriTemplate.trim();
  const documentation = value.documentationUrl.trim();
  return {
    id: value.id.trim(),
    name: value.name.trim(),
    description: value.description.trim(),
    hosting: value.hosting,
    issuerUriTemplate: template === '' ? null : template,
    parameters: value.parameters.map((parameter) => ({
      key: parameter.key.trim(),
      label: parameter.label.trim(),
      example: parameter.example.trim(),
    })),
    supportsResourceServer: value.supportsResourceServer,
    supportsLogin: value.supportsLogin,
    documentationUrl: documentation === '' ? null : documentation,
  };
}