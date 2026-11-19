import { FormArray, FormControl, FormGroup, ValidatorFn } from "@angular/forms";
import { DependencyCategory, DependencyDefinition, DependencyKind, GradleScope, GroupDefinition, MavenScope } from "../catalog.models";
import { ArtifactCoordinates } from "../../version/version.models";

/** Formato de un id estable; igual que `Preconditions.requireSlug` del backend. */
export const SLUG_PATTERN = /^[a-z][a-z0-9]*(-[a-z0-9]+)*$/;

/** Categorías que se administran sin motor; DATABASE va siempre con su motor. */
export const STANDALONE_CATEGORIES: readonly DependencyCategory[] = ['SPRING', 'OTHER', 'MAVEN', 'GRADLE'];

/** Nombre legible de cada categoría, igual que en el backend. */
export const CATEGORY_LABELS: Readonly<Record<DependencyCategory, string>> = {
  MAVEN: 'Maven',
  GRADLE: 'Gradle',
  SPRING: 'Spring',
  DATABASE: 'Database',
  OTHER: 'Other',
};

/** Formulario de una dependencia del grupo. */
export type DependencyForm = FormGroup<{
  /** Nombre legible. */
  name: FormControl<string>;
  /** groupId de Maven. */
  groupId: FormControl<string>;
  /** artifactId de Maven. */
  artifactId: FormControl<string>;
  /** Versión elegida, o `null` si la gestiona Spring Boot o un BOM. */
  version: FormControl<string | null>;
  /** Tipo de artefacto. */
  kind: FormControl<DependencyKind>;
  /** Ámbito en Maven; solo cuenta para librerías. */
  mavenScope: FormControl<MavenScope | null>;
  /** Configuración en Gradle; solo cuenta para librerías. */
  gradleScope: FormControl<GradleScope | null>;
  /** Si se incluye siempre que su grupo está activo. */
  includedByDefault: FormControl<boolean>;
  /** Si pertenece a la pila reactiva. */
  reactive: FormControl<boolean>;
}>;

/** Formulario de un grupo con sus dependencias. */
export type GroupForm = FormGroup<{
  /** Identificador estable, con formato slug. */
  id: FormControl<string>;
  /** Nombre visible. */
  name: FormControl<string>;
  /** Categoría del grupo. */
  category: FormControl<DependencyCategory>;
  /** Si aparece marcado al abrir la pantalla de selección. */
  selectedByDefault: FormControl<boolean>;
  /** Si necesita una base de datos de desarrollo. */
  requiresDatabase: FormControl<boolean>;
  /** Filas de dependencias, en orden. */
  dependencies: FormArray<DependencyForm>;
}>;

/** Valor completo de una fila de dependencia. */
export type DependencyFormValue = ReturnType<DependencyForm['getRawValue']>;

/**
 * Exige un texto con contenido; a diferencia de `Validators.required`,
 * también rechaza los espacios en blanco.
 * @param control control a validar
 * @returns `{ required: true }` si está vacío o en blanco
 */
export const notBlank: ValidatorFn = (control) =>
  typeof control.value === 'string' && control.value.trim() !== '' ? null : { required: true };

/**
 * Exige al menos un elemento en la lista.
 * @param control lista a validar
 * @returns `{ atLeastOne: true }` si está vacía
 */
export const atLeastOne: ValidatorFn = (control) =>
  Array.isArray(control.value) && control.value.length > 0 ? null : { atLeastOne: true };

/**
 * Exige los dos ámbitos cuando la dependencia es una librería.
 * @param control fila de dependencia
 * @returns `{ scopesRequired: true }` si falta alguno
 */
const libraryScopes: ValidatorFn = (control) => {
  const { kind, mavenScope, gradleScope } = (control as DependencyForm).getRawValue();
  return kind === 'LIBRARY' && (!mavenScope || !gradleScope) ? { scopesRequired: true } : null;
};

/**
 * Impide marcar una dependencia reactiva como incluida por defecto: solo se
 * activa cuando el proyecto es reactivo.
 * @param control fila de dependencia
 * @returns `{ reactiveByDefault: true }` si tiene ambas marcas
 */
const reactiveNotByDefault: ValidatorFn = (control) => {
  const { reactive, includedByDefault } = (control as DependencyForm).getRawValue();
  return reactive && includedByDefault ? { reactiveByDefault: true } : null;
};

/**
 * Crea el formulario de una dependencia.
 * @param definition dependencia existente; sin ella, una librería nueva
 *   en `COMPILE`/`IMPLEMENTATION` incluida por defecto
 * @returns formulario con las reglas de la dependencia
 */
export function dependencyForm(definition?: DependencyDefinition): DependencyForm {
  return new FormGroup(
    {
      name: new FormControl(definition?.name ?? '', { nonNullable: true, validators: notBlank }),
      groupId: new FormControl(definition?.groupId ?? '', { nonNullable: true, validators: notBlank }),
      artifactId: new FormControl(definition?.artifactId ?? '', { nonNullable: true, validators: notBlank }),
      version: new FormControl<string | null>(definition?.version ?? null),
      kind: new FormControl<DependencyKind>(definition?.kind ?? 'LIBRARY', { nonNullable: true }),
      mavenScope: new FormControl<MavenScope | null>(definition ? definition.mavenScope : 'COMPILE'),
      gradleScope: new FormControl<GradleScope | null>(definition ? definition.gradleScope : 'IMPLEMENTATION'),
      includedByDefault: new FormControl(definition?.includedByDefault ?? true, { nonNullable: true }),
      reactive: new FormControl(definition?.reactive ?? false, { nonNullable: true }),
    },
    { validators: [libraryScopes, reactiveNotByDefault] },
  );
}

/**
 * Crea el formulario de un grupo.
 * @param category categoría de un grupo nuevo; si se edita uno existente,
 *   manda la del grupo
 * @param definition grupo existente; sin él, un grupo vacío con una
 *   dependencia en blanco
 * @returns formulario con las reglas del grupo
 */
export function groupForm(category: DependencyCategory, definition?: GroupDefinition): GroupForm {
  const dependencies = definition ? definition.dependencies.map((d) => dependencyForm(d)) : [dependencyForm()];
  return new FormGroup({
    id: new FormControl(definition?.id ?? '', {
      nonNullable: true,
      validators: [notBlank, (control) => (SLUG_PATTERN.test(control.value) ? null : { pattern: true })],
    }),
    name: new FormControl(definition?.name ?? '', { nonNullable: true, validators: notBlank }),
    category: new FormControl<DependencyCategory>(definition?.category ?? category, { nonNullable: true }),
    selectedByDefault: new FormControl(definition?.selectedByDefault ?? false, { nonNullable: true }),
    requiresDatabase: new FormControl(definition?.requiresDatabase ?? false, { nonNullable: true }),
    dependencies: new FormArray(dependencies, { validators: atLeastOne }),
  });
}

/**
 * Convierte el formulario en el cuerpo que espera la API: recorta textos,
 * deja `null` la versión vacía y quita los ámbitos que no son de librería.
 * @param form formulario del grupo
 * @returns definición lista para `POST` o `PUT`
 */
export function toGroupDefinition(form: GroupForm): GroupDefinition {
  const value = form.getRawValue();
  return {
    id: value.id.trim(),
    name: value.name.trim(),
    category: value.category,
    selectedByDefault: value.selectedByDefault,
    requiresDatabase: value.requiresDatabase,
    dependencies: value.dependencies.map(toDependencyDefinition),
  };
}

/**
 * Coordenadas completas de una fila, para consultar sus versiones.
 * @param row fila de dependencia
 * @returns `groupId` y `artifactId` recortados, o `null` si falta alguno
 */
export function coordinatesOf(row: DependencyForm): ArtifactCoordinates | null {
  const groupId = row.controls.groupId.value.trim();
  const artifactId = row.controls.artifactId.value.trim();
  return groupId && artifactId ? { groupId, artifactId } : null;
}

/**
 * Convierte una fila del formulario en la dependencia que espera la API.
 * @param value valor completo de la fila
 * @returns dependencia limpia
 */
export function toDependencyDefinition(value: DependencyFormValue): DependencyDefinition {
  const library = value.kind === 'LIBRARY';
  const version = value.version?.trim() ?? '';
  return {
    name: value.name.trim(),
    groupId: value.groupId.trim(),
    artifactId: value.artifactId.trim(),
    version: version === '' ? null : version,
    kind: value.kind,
    mavenScope: library ? value.mavenScope : null,
    gradleScope: library ? value.gradleScope : null,
    includedByDefault: value.includedByDefault,
    reactive: value.reactive,
  };
}