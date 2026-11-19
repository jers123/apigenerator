import { FormControl, FormGroup, ValidatorFn, Validators } from "@angular/forms";
import { DatabaseEngine, DatabaseEngineRequest, DatabaseKind } from "../database.models";
import { groupForm, GroupForm, notBlank, SLUG_PATTERN, toGroupDefinition } from "../../catalog/group-editor/group-form";
import { GroupDefinition } from "../../catalog/catalog.models";

/** Mayor número de puerto TCP válido. */
const MAX_PORT = 65_535;

/** Formulario de un motor con su grupo de driver. */
export type EngineForm = FormGroup<{
  /** Identificador estable, con formato slug. */
  id: FormControl<string>;
  /** Nombre legible. */
  label: FormControl<string>;
  /** Tipo de base. */
  kind: FormControl<DatabaseKind>;
  /** Si tiene secuencias. */
  supportsSequences: FormControl<boolean>;
  /** Si corre dentro de la aplicación, sin servidor. */
  embedded: FormControl<boolean>;
  /** Puerto por defecto; obligatorio si no es embebida. */
  defaultPort: FormControl<number | null>;
  /** Usuario por defecto. */
  defaultUsername: FormControl<string>;
  /** Plantilla de la URL JDBC. */
  jdbcUrlTemplate: FormControl<string>;
  /** Grupo de base de datos con el driver. */
  driverGroup: GroupForm;
}>;

/**
 * Exige el puerto cuando el motor tiene servidor.
 * @param control formulario del motor
 * @returns `{ portRequired: true }` si no es embebida y no tiene puerto
 */
const portWhenServer: ValidatorFn = (control) => {
  const { embedded, defaultPort } = (control as EngineForm).getRawValue();
  return !embedded && defaultPort === null ? { portRequired: true } : null;
};

/**
 * Crea el formulario de un motor.
 * @param engine motor existente; sin él, uno nuevo relacional, con servidor y sin secuencias
 * @param driverGroup grupo de driver del motor existente; sin él, uno nuevo
 * @returns formulario con las reglas del motor y de su grupo
 */
export function engineForm(engine?: DatabaseEngine, driverGroup?: GroupDefinition): EngineForm {
  return new FormGroup(
    {
      id: new FormControl(engine?.id ?? '', {
        nonNullable: true,
        validators: [notBlank, Validators.pattern(SLUG_PATTERN)],
      }),
      label: new FormControl(engine?.label ?? '', { nonNullable: true, validators: notBlank }),
      kind: new FormControl<DatabaseKind>(engine?.kind ?? 'RELATIONAL', { nonNullable: true }),
      supportsSequences: new FormControl(engine?.supportsSequences ?? false, { nonNullable: true }),
      embedded: new FormControl(engine?.embedded ?? false, { nonNullable: true }),
      defaultPort: new FormControl<number | null>(engine?.defaultPort ?? null, {
        validators: [Validators.min(1), Validators.max(MAX_PORT)],
      }),
      defaultUsername: new FormControl(engine?.defaultUsername ?? '', { nonNullable: true, validators: notBlank }),
      jdbcUrlTemplate: new FormControl(engine?.jdbcUrlTemplate ?? '', { nonNullable: true, validators: notBlank }),
      driverGroup: groupForm('DATABASE', driverGroup),
    },
    { validators: portWhenServer },
  );
}

/**
 * Convierte el formulario en el cuerpo que espera la API. Si el motor es
 * embebido, el puerto se envía como `null`.
 * @param form formulario del motor
 * @returns petición lista para `POST` o `PUT`
 */
export function toEngineRequest(form: EngineForm): DatabaseEngineRequest {
  const value = form.getRawValue();
  return {
    id: value.id.trim(),
    label: value.label.trim(),
    embedded: value.embedded,
    defaultPort: value.embedded ? null : value.defaultPort,
    defaultUsername: value.defaultUsername.trim(),
    jdbcUrlTemplate: value.jdbcUrlTemplate.trim(),
    driverGroup: toGroupDefinition(form.controls.driverGroup),
    kind: value.kind,
    supportsSequences: value.supportsSequences,
  };
}