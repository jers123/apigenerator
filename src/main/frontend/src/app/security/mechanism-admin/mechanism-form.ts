import { FormArray, FormControl, FormGroup, ValidatorFn, Validators } from "@angular/forms";
import { ISSUER_ROLES, SecurityMechanismDefinition, SecurityRole } from "../security.models";
import { atLeastOne, dependencyForm, DependencyForm, notBlank, SLUG_PATTERN, toDependencyDefinition } from "../../catalog/group-editor/group-form";

/** Formulario de un método de seguridad. */
export type MechanismForm = FormGroup<{
  /** Identificador estable, con formato slug. */
  id: FormControl<string>;
  /** Nombre visible. */
  name: FormControl<string>;
  /** Explicación breve. */
  description: FormControl<string>;
  /** Para qué sirve. */
  role: FormControl<SecurityRole>;
  /** Si funciona en proyectos reactivos. */
  reactiveSupported: FormControl<boolean>;
  /** Si necesita un emisor de tokens. */
  requiresIssuer: FormControl<boolean>;
  /** Si el proyecto debe guardar usuarios. */
  managesUsers: FormControl<boolean>;
  /** Ids de los métodos con los que choca. */
  conflictsWith: FormControl<string[]>;
  /** Artefactos que agrega, en orden. */
  dependencies: FormArray<DependencyForm>;
}>;

/**
 * Indica si un tipo de método puede necesitar emisor.
 * @param role tipo del método
 * @returns `true` para la autenticación de la API y el inicio de sesión
 */
export function mayRequireIssuer(role: SecurityRole): boolean {
  return ISSUER_ROLES.includes(role);
}

/**
 * Solo la autenticación de la API y el inicio de sesión pueden necesitar emisor.
 * @param control formulario del método
 * @returns `{ issuerRole: true }` si otro tipo lo pide
 */
const issuerOnlyForAuthentication: ValidatorFn = (control) => {
  const { role, requiresIssuer } = (control as MechanismForm).getRawValue();
  return requiresIssuer && !mayRequireIssuer(role) ? { issuerRole: true } : null;
};

/**
 * Crea el formulario de un método.
 * @param definition método existente; sin él, una autenticación de la API
 *   reactiva con una dependencia en blanco
 * @returns formulario con las reglas del método
 */
export function mechanismForm(definition?: SecurityMechanismDefinition): MechanismForm {
  const dependencies = definition ? definition.dependencies.map((d) => dependencyForm(d)) : [dependencyForm()];
  return new FormGroup(
    {
      id: new FormControl(definition?.id ?? '', {
        nonNullable: true,
        validators: [notBlank, Validators.pattern(SLUG_PATTERN)],
      }),
      name: new FormControl(definition?.name ?? '', { nonNullable: true, validators: notBlank }),
      description: new FormControl(definition?.description ?? '', { nonNullable: true }),
      role: new FormControl<SecurityRole>(definition?.role ?? 'API_AUTHENTICATION', { nonNullable: true }),
      reactiveSupported: new FormControl(definition?.reactiveSupported ?? true, { nonNullable: true }),
      requiresIssuer: new FormControl(definition?.requiresIssuer ?? false, { nonNullable: true }),
      managesUsers: new FormControl(definition?.managesUsers ?? false, { nonNullable: true }),
      conflictsWith: new FormControl<string[]>([...(definition?.conflictsWith ?? [])], { nonNullable: true }),
      dependencies: new FormArray(dependencies, { validators: atLeastOne }),
    },
    { validators: issuerOnlyForAuthentication },
  );
}

/**
 * Convierte el formulario en el cuerpo que espera la API.
 * @param form formulario del método
 * @returns definición lista para `POST` o `PUT`, con los choques ordenados
 */
export function toMechanismDefinition(form: MechanismForm): SecurityMechanismDefinition {
  const value = form.getRawValue();
  return {
    id: value.id.trim(),
    name: value.name.trim(),
    description: value.description.trim(),
    role: value.role,
    reactiveSupported: value.reactiveSupported,
    requiresIssuer: value.requiresIssuer,
    managesUsers: value.managesUsers,
    conflictsWith: [...value.conflictsWith].sort(),
    dependencies: value.dependencies.map(toDependencyDefinition),
  };
}