import { FormArray, FormControl, FormGroup } from "@angular/forms";
import { Attribute, AttributeConstraints, AuditFields, CARDINALITIES, Cardinality, DomainModel, EMPTY_DOMAIN_MODEL, Entity, ID_GENERATIONS, IdGeneration, IdGenerationOption, IdPlacement, Relation, SCALAR_TYPES, ScalarType, ScalarTypeOption, Superclass, TemporalRule } from "./model.models";

/** Tipo de elemento del modelo que lleva id interno. */
export type ModelElementKind = 'superclass' | 'entity' | 'attribute' | 'relation';

/** Prefijo del id interno de cada tipo de elemento, para reconocerlo al depurar. */
const ID_PREFIXES: Readonly<Record<ModelElementKind, string>> = {
  superclass: 's',
  entity: 'e',
  attribute: 'a',
  relation: 'r',
};

/** Restricciones vacías. */
export const NO_CONSTRAINTS: AttributeConstraints = {
  minLength: null,
  maxLength: null,
  min: null,
  max: null,
  integerDigits: null,
  fractionDigits: null,
  pattern: null,
  email: false,
  temporal: null,
};

/** Tipos que pueden ser id, en el orden del backend. */
export const ID_TYPES: readonly ScalarTypeOption[] = SCALAR_TYPES.filter((type) => type.idCapable);

/** Qué grupos de restricciones admite un tipo; igual que `ScalarType.accepts*` del backend. */
export interface ConstraintRules {
  /** Longitud, patrón y email: solo textos. */
  readonly text: boolean;
  /** Mínimo y máximo: enteros y decimales. */
  readonly range: boolean;
  /** Dígitos enteros y decimales: solo `BigDecimal`. */
  readonly digits: boolean;
  /** Pasado o futuro: fechas e instantes. */
  readonly temporal: boolean;
}

/** Formulario de las restricciones de un atributo. */
export type ConstraintsForm = FormGroup<{
  /** Longitud mínima de un texto. */
  minLength: FormControl<number | null>;
  /** Longitud máxima de un texto. */
  maxLength: FormControl<number | null>;
  /** Valor mínimo de un número. */
  min: FormControl<number | null>;
  /** Valor máximo de un número. */
  max: FormControl<number | null>;
  /** Dígitos enteros de un decimal. */
  integerDigits: FormControl<number | null>;
  /** Dígitos decimales de un decimal. */
  fractionDigits: FormControl<number | null>;
  /** Expresión regular; vacía equivale a ninguna. */
  pattern: FormControl<string>;
  /** Si un texto debe ser un correo. */
  email: FormControl<boolean>;
  /** Restricción de tiempo de una fecha. */
  temporal: FormControl<TemporalRule | null>;
}>;

/** Formulario de un atributo. */
export type AttributeForm = FormGroup<{
  /** Id interno; no se edita. */
  id: FormControl<string>;
  /** Nombre en camelCase. */
  name: FormControl<string>;
  /** Columna escrita; vacía para derivarla. */
  columnName: FormControl<string>;
  /** Tipo simple. */
  type: FormControl<ScalarType>;
  /** Si es obligatorio. */
  required: FormControl<boolean>;
  /** Si su valor no se puede repetir. */
  unique: FormControl<boolean>;
  /** Restricciones. */
  constraints: ConstraintsForm;
}>;

/** Formulario de una relación. */
export type RelationForm = FormGroup<{
  /** Id interno; no se edita. */
  id: FormControl<string>;
  /** Nombre en camelCase. */
  name: FormControl<string>;
  /** Id interno de la entidad destino; vacío si aún no se elige. */
  targetEntityId: FormControl<string>;
  /** Cardinalidad vista desde la entidad que la declara. */
  cardinality: FormControl<Cardinality>;
  /** Si es obligatoria; solo vale en 1‑1 y N‑1. */
  required: FormControl<boolean>;
  /** Nombre del lado inverso; vacío si es unidireccional. */
  inverseName: FormControl<string>;
  /** Columna de la clave foránea escrita; vacía para derivarla. */
  joinColumn: FormControl<string>;
  /** Tabla intermedia escrita (solo N‑N); vacía para derivarla. */
  joinTable: FormControl<string>;
}>;

/** Formulario de una superclase. */
export type SuperclassForm = FormGroup<{
  /** Id interno; no se edita. */
  id: FormControl<string>;
  /** Nombre en PascalCase. */
  name: FormControl<string>;
  /** Id interno de la superclase padre, o `null` si es raíz. */
  parentId: FormControl<string | null>;
  /** Atributos que aporta. */
  attributes: FormArray<AttributeForm>;
}>;

/** Formulario de una entidad. */
export type EntityForm = FormGroup<{
  /** Id interno; no se edita. */
  id: FormControl<string>;
  /** Nombre en PascalCase. */
  name: FormControl<string>;
  /** Tabla escrita; vacía para derivarla. */
  tableName: FormControl<string>;
  /** Ruta REST escrita; vacía para derivarla. */
  resourcePath: FormControl<string>;
  /** Secuencia escrita; vacía para derivarla. */
  sequenceName: FormControl<string>;
  /** Id interno de la superclase, o `null` si no hereda. */
  superclassId: FormControl<string | null>;
  /** Atributos propios. */
  attributes: FormArray<AttributeForm>;
  /** Relaciones que declara. */
  relations: FormArray<RelationForm>;
}>;

/** Formulario de la configuración del id. */
export type IdSettingsForm = FormGroup<{
  /** Tipo del id. */
  type: FormControl<ScalarType>;
  /** Cómo se genera. */
  generation: FormControl<IdGeneration>;
  /** Dónde se declaran el id y la auditoría. */
  placement: FormControl<IdPlacement>;
}>;

/** Formulario de la auditoría. */
export type AuditForm = FormGroup<{
  /** Si se agrega la fecha de creación. */
  createdAt: FormControl<boolean>;
  /** Si se agrega la fecha de modificación. */
  updatedAt: FormControl<boolean>;
}>;

/** Formulario del modelo completo. */
export type ModelForm = FormGroup<{
  /** Configuración del id. */
  idSettings: IdSettingsForm;
  /** Auditoría. */
  audit: AuditForm;
  /** Superclases, en orden. */
  superclasses: FormArray<SuperclassForm>;
  /** Entidades, en orden. */
  entities: FormArray<EntityForm>;
}>;

/** Valor completo de una superclase del formulario. */
export type SuperclassFormValue = ReturnType<SuperclassForm['getRawValue']>;

/** Valor completo de un atributo del formulario. */
type AttributeFormValue = ReturnType<AttributeForm['getRawValue']>;

/** Valor completo de una relación del formulario. */
type RelationFormValue = ReturnType<RelationForm['getRawValue']>;

/** Valor completo de una entidad del formulario. */
type EntityFormValue = ReturnType<EntityForm['getRawValue']>;

// ── Ids y reglas ─────────────────────────────────────────────────────

/**
 * Crea un id interno nuevo.
 * @param kind tipo de elemento
 * @returns prefijo del tipo y un UUID, por ejemplo `e-3f25…`
 */
export function newElementId(kind: ModelElementKind): string {
  return `${ID_PREFIXES[kind]}-${crypto.randomUUID()}`;
}

/**
 * Qué restricciones admite un tipo.
 * @param type tipo simple
 * @returns grupos de restricciones permitidos
 */
export function constraintRules(type: ScalarType): ConstraintRules {
  const family = SCALAR_TYPES.find((option) => option.value === type)?.family;
  return {
    text: family === 'TEXT',
    range: family === 'INTEGRAL' || family === 'DECIMAL' || family === 'FLOATING',
    digits: family === 'DECIMAL',
    temporal: family === 'TEMPORAL',
  };
}

/**
 * Estrategias de id que funcionan con un tipo.
 * @param type tipo del id
 * @returns estrategias compatibles, en el orden del backend
 */
export function compatibleGenerations(type: ScalarType): readonly IdGenerationOption[] {
  return ID_GENERATIONS.filter((generation) => generation.compatibleTypes.includes(type));
}

/**
 * Indica si una cardinalidad tiene una colección del lado que la declara.
 * @param cardinality cardinalidad
 * @returns `true` para 1‑N y N‑N
 */
export function isToMany(cardinality: Cardinality): boolean {
  return CARDINALITIES.find((option) => option.value === cardinality)?.toMany ?? false;
}

// ── Creación de formularios ──────────────────────────────────────────

/**
 * Crea el formulario de unas restricciones.
 * @param constraints restricciones existentes; sin ellas, ninguna
 * @returns formulario de restricciones
 */
export function constraintsForm(constraints: AttributeConstraints = NO_CONSTRAINTS): ConstraintsForm {
  return new FormGroup({
    minLength: new FormControl<number | null>(constraints.minLength),
    maxLength: new FormControl<number | null>(constraints.maxLength),
    min: new FormControl<number | null>(constraints.min),
    max: new FormControl<number | null>(constraints.max),
    integerDigits: new FormControl<number | null>(constraints.integerDigits),
    fractionDigits: new FormControl<number | null>(constraints.fractionDigits),
    pattern: new FormControl(constraints.pattern ?? '', { nonNullable: true }),
    email: new FormControl(constraints.email, { nonNullable: true }),
    temporal: new FormControl<TemporalRule | null>(constraints.temporal),
  });
}

/**
 * Crea el formulario de un atributo.
 * @param attribute atributo existente; sin él, un `String` opcional nuevo con id propio
 * @returns formulario del atributo
 */
export function attributeForm(attribute?: Attribute): AttributeForm {
  return new FormGroup({
    id: new FormControl(attribute?.id ?? newElementId('attribute'), { nonNullable: true }),
    name: new FormControl(attribute?.name ?? '', { nonNullable: true }),
    columnName: new FormControl(attribute?.columnName ?? '', { nonNullable: true }),
    type: new FormControl<ScalarType>(attribute?.type.scalar ?? 'STRING', { nonNullable: true }),
    required: new FormControl(attribute?.required ?? false, { nonNullable: true }),
    unique: new FormControl(attribute?.unique ?? false, { nonNullable: true }),
    constraints: constraintsForm(attribute?.constraints),
  });
}

/**
 * Crea el formulario de una relación.
 * @param relation relación existente; sin ella, una N‑1 opcional nueva con id propio
 * @param targetEntityId destino de una relación nueva; se ignora si se pasa una existente
 * @returns formulario de la relación
 */
export function relationForm(relation?: Relation, targetEntityId = ''): RelationForm {
  return new FormGroup({
    id: new FormControl(relation?.id ?? newElementId('relation'), { nonNullable: true }),
    name: new FormControl(relation?.name ?? '', { nonNullable: true }),
    targetEntityId: new FormControl(relation?.targetEntityId ?? targetEntityId, { nonNullable: true }),
    cardinality: new FormControl<Cardinality>(relation?.cardinality ?? 'MANY_TO_ONE', { nonNullable: true }),
    required: new FormControl(relation?.required ?? false, { nonNullable: true }),
    inverseName: new FormControl(relation?.inverseName ?? '', { nonNullable: true }),
    joinColumn: new FormControl(relation?.joinColumn ?? '', { nonNullable: true }),
    joinTable: new FormControl(relation?.joinTable ?? '', { nonNullable: true }),
  });
}

/**
 * Crea el formulario de una superclase.
 * @param superclass superclase existente; sin ella, una nueva sin atributos con id propio
 * @param parentId padre de una superclase nueva; se ignora si se pasa una existente
 * @returns formulario de la superclase
 */
export function superclassForm(superclass?: Superclass, parentId: string | null = null): SuperclassForm {
  return new FormGroup({
    id: new FormControl(superclass?.id ?? newElementId('superclass'), { nonNullable: true }),
    name: new FormControl(superclass?.name ?? '', { nonNullable: true }),
    parentId: new FormControl<string | null>(superclass ? superclass.parentId : parentId),
    attributes: new FormArray((superclass?.attributes ?? []).map((attribute) => attributeForm(attribute))),
  });
}

/**
 * Crea el formulario de una entidad.
 * @param entity entidad existente; sin ella, una nueva vacía con id propio
 * @returns formulario de la entidad
 */
export function entityForm(entity?: Entity): EntityForm {
  return new FormGroup({
    id: new FormControl(entity?.id ?? newElementId('entity'), { nonNullable: true }),
    name: new FormControl(entity?.name ?? '', { nonNullable: true }),
    tableName: new FormControl(entity?.tableName ?? '', { nonNullable: true }),
    resourcePath: new FormControl(entity?.resourcePath ?? '', { nonNullable: true }),
    sequenceName: new FormControl(entity?.sequenceName ?? '', { nonNullable: true }),
    superclassId: new FormControl<string | null>(entity?.superclassId ?? null),
    attributes: new FormArray((entity?.attributes ?? []).map((attribute) => attributeForm(attribute))),
    relations: new FormArray((entity?.relations ?? []).map((relation) => relationForm(relation))),
  });
}

/**
 * Crea el formulario del modelo completo.
 * @param model modelo existente; sin él, el modelo vacío por defecto
 * @returns formulario del modelo
 */
export function modelForm(model: DomainModel = EMPTY_DOMAIN_MODEL): ModelForm {
  return new FormGroup({
    idSettings: new FormGroup({
      type: new FormControl<ScalarType>(model.idSettings.type, { nonNullable: true }),
      generation: new FormControl<IdGeneration>(model.idSettings.generation, { nonNullable: true }),
      placement: new FormControl<IdPlacement>(model.idSettings.placement, { nonNullable: true }),
    }),
    audit: new FormGroup({
      createdAt: new FormControl(model.audit.createdAt, { nonNullable: true }),
      updatedAt: new FormControl(model.audit.updatedAt, { nonNullable: true }),
    }),
    superclasses: new FormArray(model.superclasses.map((superclass) => superclassForm(superclass))),
    entities: new FormArray(model.entities.map((entity) => entityForm(entity))),
  });
}

// ── Conversión a la API ──────────────────────────────────────────────

/**
 * Convierte el formulario en el modelo que espera la API: recorta los
 * nombres y deja `null` los textos opcionales vacíos.
 * @param form formulario del modelo
 * @returns modelo listo para validar o planificar
 */
export function toDomainModel(form: ModelForm): DomainModel {
  const value = form.getRawValue();
  const audit: AuditFields = { createdAt: value.audit.createdAt, updatedAt: value.audit.updatedAt };
  return {
    idSettings: { ...value.idSettings },
    audit,
    superclasses: value.superclasses.map(toSuperclass),
    entities: value.entities.map(toEntity),
  };
}

/**
 * Recorta un texto opcional.
 * @param value texto del formulario
 * @returns el texto recortado, o `null` si queda vacío
 */
function optionalText(value: string): string | null {
  const trimmed = value.trim();
  return trimmed === '' ? null : trimmed;
}

/**
 * Convierte una superclase del formulario.
 * @param value valor de la superclase
 * @returns superclase para la API
 */
function toSuperclass(value: SuperclassFormValue): Superclass {
  return {
    id: value.id,
    name: value.name.trim(),
    parentId: value.parentId,
    attributes: value.attributes.map(toAttribute),
  };
}

/**
 * Convierte una entidad del formulario.
 * @param value valor de la entidad
 * @returns entidad para la API
 */
function toEntity(value: EntityFormValue): Entity {
  return {
    id: value.id,
    name: value.name.trim(),
    tableName: optionalText(value.tableName),
    resourcePath: optionalText(value.resourcePath),
    sequenceName: optionalText(value.sequenceName),
    superclassId: value.superclassId,
    attributes: value.attributes.map(toAttribute),
    relations: value.relations.map(toRelation),
  };
}

/**
 * Convierte un atributo del formulario.
 * @param value valor del atributo
 * @returns atributo para la API
 */
function toAttribute(value: AttributeFormValue): Attribute {
  const c = value.constraints;
  return {
    id: value.id,
    name: value.name.trim(),
    columnName: optionalText(value.columnName),
    type: { scalar: value.type, enumId: null },
    required: value.required,
    unique: value.unique,
    constraints: {
      minLength: c.minLength,
      maxLength: c.maxLength,
      min: c.min,
      max: c.max,
      integerDigits: c.integerDigits,
      fractionDigits: c.fractionDigits,
      pattern: optionalText(c.pattern),
      email: c.email,
      temporal: c.temporal,
    },
  };
}

/**
 * Convierte una relación del formulario.
 * @param value valor de la relación
 * @returns relación para la API
 */
function toRelation(value: RelationFormValue): Relation {
  return {
    id: value.id,
    name: value.name.trim(),
    targetEntityId: value.targetEntityId,
    cardinality: value.cardinality,
    required: value.required,
    inverseName: optionalText(value.inverseName),
    joinColumn: optionalText(value.joinColumn),
    joinTable: optionalText(value.joinTable),
  };
}

// ── Edición ──────────────────────────────────────────────────────────

/**
 * Agrega una entidad nueva al final.
 * @param form formulario del modelo
 * @returns formulario de la entidad agregada
 */
export function addEntity(form: ModelForm): EntityForm {
  const entity = entityForm();
  form.controls.entities.push(entity);
  return entity;
}

/**
 * Cuenta las relaciones de otras entidades que apuntan a una entidad; se
 * borran con ella, así que la pantalla las anuncia antes de confirmar.
 * @param form formulario del modelo
 * @param entityId id interno de la entidad
 * @returns cuántas relaciones de otras entidades la tienen como destino
 */
export function relationsPointingTo(form: ModelForm, entityId: string): number {
  return form.controls.entities.controls
    .filter((entity) => entity.controls.id.value !== entityId)
    .flatMap((entity) => entity.controls.relations.controls)
    .filter((relation) => relation.controls.targetEntityId.value === entityId).length;
}

/**
 * Borra una entidad y las relaciones de otras entidades que apuntan a ella.
 * @param form formulario del modelo
 * @param entityId id interno de la entidad
 */
export function removeEntity(form: ModelForm, entityId: string): void {
  const entities = form.controls.entities;
  const index = entities.controls.findIndex((entity) => entity.controls.id.value === entityId);
  if (index < 0) {
    return;
  }
  entities.removeAt(index);
  for (const entity of entities.controls) {
    const relations = entity.controls.relations;
    for (let i = relations.length - 1; i >= 0; i--) {
      if (relations.at(i).controls.targetEntityId.value === entityId) {
        relations.removeAt(i);
      }
    }
  }
}

/**
 * Agrega una superclase nueva al final.
 * @param form formulario del modelo
 * @param parentId superclase padre, o `null` para crearla como raíz
 * @returns formulario de la superclase agregada
 */
export function addSuperclass(form: ModelForm, parentId: string | null = null): SuperclassForm {
  const superclass = superclassForm(undefined, parentId);
  form.controls.superclasses.push(superclass);
  return superclass;
}

/**
 * Borra una superclase sin romper el árbol: sus superclases hijas y sus
 * entidades pasan a heredar del padre de la borrada (o de nadie si era raíz).
 * @param form formulario del modelo
 * @param superclassId id interno de la superclase
 */
export function removeSuperclass(form: ModelForm, superclassId: string): void {
  const superclasses = form.controls.superclasses;
  const index = superclasses.controls.findIndex((superclass) => superclass.controls.id.value === superclassId);
  if (index < 0) {
    return;
  }
  const parentId = superclasses.at(index).controls.parentId.value;
  superclasses.removeAt(index);
  for (const superclass of superclasses.controls) {
    if (superclass.controls.parentId.value === superclassId) {
      superclass.controls.parentId.setValue(parentId);
    }
  }
  for (const entity of form.controls.entities.controls) {
    if (entity.controls.superclassId.value === superclassId) {
      entity.controls.superclassId.setValue(parentId);
    }
  }
}

/**
 * Superclases que pueden ser padre de otra: todas menos ella misma y sus
 * descendientes, para no ofrecer ciclos.
 * @param form formulario del modelo
 * @param superclassId id interno de la superclase que elige padre
 * @returns candidatas, en el orden del modelo
 */
export function parentCandidates(form: ModelForm, superclassId: string): readonly SuperclassFormValue[] {
  const all = form.controls.superclasses.getRawValue();
  const excluded = new Set<string>([superclassId]);
  let grew = true;
  while (grew) {
    grew = false;
    for (const superclass of all) {
      if (superclass.parentId !== null && excluded.has(superclass.parentId) && !excluded.has(superclass.id)) {
        excluded.add(superclass.id);
        grew = true;
      }
    }
  }
  return all.filter((superclass) => !excluded.has(superclass.id));
}

/**
 * Agrega un atributo nuevo al final de una lista.
 * @param attributes atributos de una entidad o superclase
 * @returns formulario del atributo agregado
 */
export function addAttribute(attributes: FormArray<AttributeForm>): AttributeForm {
  const attribute = attributeForm();
  attributes.push(attribute);
  return attribute;
}

/**
 * Agrega una relación nueva al final de una entidad.
 * @param entity entidad que la declara
 * @param targetEntityId entidad destino propuesta; vacía si aún no se elige
 * @returns formulario de la relación agregada
 */
export function addRelation(entity: EntityForm, targetEntityId = ''): RelationForm {
  const relation = relationForm(undefined, targetEntityId);
  entity.controls.relations.push(relation);
  return relation;
}

/**
 * Limpia las restricciones que el tipo actual del atributo no admite; se
 * llama al cambiar el tipo.
 * @param attribute formulario del atributo
 */
export function clearUnsupportedConstraints(attribute: AttributeForm): void {
  const rules = constraintRules(attribute.controls.type.value);
  const c = attribute.controls.constraints.controls;
  if (!rules.text) {
    c.minLength.setValue(null);
    c.maxLength.setValue(null);
    c.pattern.setValue('');
    c.email.setValue(false);
  }
  if (!rules.range) {
    c.min.setValue(null);
    c.max.setValue(null);
  }
  if (!rules.digits) {
    c.integerDigits.setValue(null);
    c.fractionDigits.setValue(null);
  }
  if (!rules.temporal) {
    c.temporal.setValue(null);
  }
}

/**
 * Ajusta una relación a su cardinalidad; se llama al cambiarla. Una
 * colección no puede ser obligatoria y solo N‑N usa tabla intermedia.
 * @param relation formulario de la relación
 */
export function applyCardinalityRules(relation: RelationForm): void {
  const cardinality = relation.controls.cardinality.value;
  if (isToMany(cardinality)) {
    relation.controls.required.setValue(false);
  }
  if (cardinality !== 'MANY_TO_MANY') {
    relation.controls.joinTable.setValue('');
  }
}