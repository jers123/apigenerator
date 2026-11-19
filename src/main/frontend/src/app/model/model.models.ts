/** Tipo simple de un atributo (espejo de `ScalarType`). */
export type ScalarType =
  | 'STRING'
  | 'INTEGER'
  | 'LONG'
  | 'BIG_DECIMAL'
  | 'DOUBLE'
  | 'BOOLEAN'
  | 'LOCAL_DATE'
  | 'LOCAL_DATE_TIME'
  | 'LOCAL_TIME'
  | 'INSTANT'
  | 'UUID';

/** Familia de un tipo simple; decide qué restricciones admite (espejo de `ScalarType.Family`). */
export type ScalarFamily =
  | 'TEXT'
  | 'INTEGRAL'
  | 'DECIMAL'
  | 'FLOATING'
  | 'BOOLEAN'
  | 'TEMPORAL'
  | 'TIME'
  | 'IDENTIFIER';

/** Opción de tipo simple para los formularios. */
export interface ScalarTypeOption {
  /** Valor que se envía a la API. */
  readonly value: ScalarType;
  /** Nombre de la clase Java, que es lo que se muestra. */
  readonly label: string;
  /** Familia del tipo. */
  readonly family: ScalarFamily;
  /** Si puede ser el tipo del id. */
  readonly idCapable: boolean;
}

/** Tipos simples en el orden del backend. */
export const SCALAR_TYPES: readonly ScalarTypeOption[] = [
  { value: 'STRING', label: 'String', family: 'TEXT', idCapable: true },
  { value: 'INTEGER', label: 'Integer', family: 'INTEGRAL', idCapable: true },
  { value: 'LONG', label: 'Long', family: 'INTEGRAL', idCapable: true },
  { value: 'BIG_DECIMAL', label: 'BigDecimal', family: 'DECIMAL', idCapable: false },
  { value: 'DOUBLE', label: 'Double', family: 'FLOATING', idCapable: false },
  { value: 'BOOLEAN', label: 'Boolean', family: 'BOOLEAN', idCapable: false },
  { value: 'LOCAL_DATE', label: 'LocalDate', family: 'TEMPORAL', idCapable: false },
  { value: 'LOCAL_DATE_TIME', label: 'LocalDateTime', family: 'TEMPORAL', idCapable: false },
  { value: 'LOCAL_TIME', label: 'LocalTime', family: 'TIME', idCapable: false },
  { value: 'INSTANT', label: 'Instant', family: 'TEMPORAL', idCapable: false },
  { value: 'UUID', label: 'UUID', family: 'IDENTIFIER', idCapable: true },
];

/** Cómo se genera el id (espejo de `IdGeneration`). */
export type IdGeneration = 'IDENTITY' | 'SEQUENCE' | 'UUID' | 'ASSIGNED';

/** Opción de estrategia de id para los formularios. */
export interface IdGenerationOption {
  /** Valor que se envía a la API. */
  readonly value: IdGeneration;
  /** Nombre legible. */
  readonly label: string;
  /** Tipos de id con los que funciona. */
  readonly compatibleTypes: readonly ScalarType[];
  /** Si necesita un motor con secuencias. */
  readonly needsSequences: boolean;
}

/** Estrategias de id en el orden del backend. */
export const ID_GENERATIONS: readonly IdGenerationOption[] = [
  { value: 'IDENTITY', label: 'Autoincremento (IDENTITY)', compatibleTypes: ['INTEGER', 'LONG'], needsSequences: false },
  { value: 'SEQUENCE', label: 'Secuencia', compatibleTypes: ['INTEGER', 'LONG'], needsSequences: true },
  { value: 'UUID', label: 'UUID generado', compatibleTypes: ['UUID', 'STRING'], needsSequences: false },
  {
    value: 'ASSIGNED',
    label: 'Asignado por la aplicación',
    compatibleTypes: ['STRING', 'INTEGER', 'LONG', 'UUID'],
    needsSequences: false,
  },
];

/** Dónde se declaran el id y la auditoría (espejo de `IdPlacement`). */
export type IdPlacement = 'ENTITY' | 'ROOT_SUPERCLASS';

/** Opción de ubicación del id para los formularios. */
export interface IdPlacementOption {
  /** Valor que se envía a la API. */
  readonly value: IdPlacement;
  /** Nombre legible. */
  readonly label: string;
}

/** Ubicaciones del id en el orden del backend. */
export const ID_PLACEMENTS: readonly IdPlacementOption[] = [
  { value: 'ENTITY', label: 'En cada entidad' },
  { value: 'ROOT_SUPERCLASS', label: 'En la superclase raíz' },
];

/** Cardinalidad vista desde la entidad que declara la relación (espejo de `Cardinality`). */
export type Cardinality = 'ONE_TO_ONE' | 'MANY_TO_ONE' | 'ONE_TO_MANY' | 'MANY_TO_MANY';

/** Opción de cardinalidad para los formularios. */
export interface CardinalityOption {
  /** Valor que se envía a la API. */
  readonly value: Cardinality;
  /** Nombre corto, por ejemplo `N‑1`. */
  readonly label: string;
  /** Si del lado que declara hay una colección; las colecciones no pueden ser obligatorias. */
  readonly toMany: boolean;
}

/** Cardinalidades en el orden del backend. */
export const CARDINALITIES: readonly CardinalityOption[] = [
  { value: 'ONE_TO_ONE', label: '1‑1', toMany: false },
  { value: 'MANY_TO_ONE', label: 'N‑1', toMany: false },
  { value: 'ONE_TO_MANY', label: '1‑N', toMany: true },
  { value: 'MANY_TO_MANY', label: 'N‑N', toMany: true },
];

/** Restricción de tiempo de una fecha o instante (espejo de `TemporalRule`). */
export type TemporalRule = 'PAST' | 'PAST_OR_PRESENT' | 'FUTURE' | 'FUTURE_OR_PRESENT';

/** Opción de restricción de tiempo para los formularios. */
export interface TemporalRuleOption {
  /** Valor que se envía a la API. */
  readonly value: TemporalRule;
  /** Nombre legible. */
  readonly label: string;
}

/** Restricciones de tiempo en el orden del backend. */
export const TEMPORAL_RULES: readonly TemporalRuleOption[] = [
  { value: 'PAST', label: 'En el pasado' },
  { value: 'PAST_OR_PRESENT', label: 'En el pasado o ahora' },
  { value: 'FUTURE', label: 'En el futuro' },
  { value: 'FUTURE_OR_PRESENT', label: 'En el futuro o ahora' },
];

/** Tipo de un atributo: un tipo simple o un enum del modelo (espejo de `AttributeType`). */
export interface AttributeType {
  /** Tipo simple, o `null` si usa un enum. */
  readonly scalar: ScalarType | null;
  /** Id interno del enum, o `null` si es simple; los enums aún no están disponibles. */
  readonly enumId: string | null;
}

/** Restricciones de un atributo; cada una es opcional (espejo de `AttributeConstraints`). */
export interface AttributeConstraints {
  /** Longitud mínima de un texto. */
  readonly minLength: number | null;
  /** Longitud máxima de un texto. */
  readonly maxLength: number | null;
  /** Valor mínimo de un número. */
  readonly min: number | null;
  /** Valor máximo de un número. */
  readonly max: number | null;
  /** Dígitos enteros de un decimal. */
  readonly integerDigits: number | null;
  /** Dígitos decimales de un decimal. */
  readonly fractionDigits: number | null;
  /** Expresión regular que debe cumplir un texto. */
  readonly pattern: string | null;
  /** Si un texto debe ser un correo. */
  readonly email: boolean;
  /** Restricción de tiempo de una fecha. */
  readonly temporal: TemporalRule | null;
}

/** Atributo simple de una entidad o superclase (espejo de `Attribute`). */
export interface Attribute {
  /** Id interno estable; lo crea la pantalla. */
  readonly id: string;
  /** Nombre en camelCase. */
  readonly name: string;
  /** Columna escrita por el usuario, o `null` para derivarla. */
  readonly columnName: string | null;
  /** Tipo del atributo. */
  readonly type: AttributeType;
  /** Si es obligatorio. */
  readonly required: boolean;
  /** Si su valor no se puede repetir. */
  readonly unique: boolean;
  /** Restricciones. */
  readonly constraints: AttributeConstraints;
}

/** Relación de una entidad con otra (espejo de `Relation`). */
export interface Relation {
  /** Id interno estable; lo crea la pantalla. */
  readonly id: string;
  /** Nombre en camelCase. */
  readonly name: string;
  /** Id interno de la entidad destino. */
  readonly targetEntityId: string;
  /** Cardinalidad vista desde la entidad que la declara. */
  readonly cardinality: Cardinality;
  /** Si es obligatoria; solo vale en 1‑1 y N‑1. */
  readonly required: boolean;
  /** Nombre del lado inverso, o `null` si es unidireccional. */
  readonly inverseName: string | null;
  /** Columna de la clave foránea escrita por el usuario, o `null` para derivarla. */
  readonly joinColumn: string | null;
  /** Tabla intermedia escrita por el usuario (solo N‑N), o `null` para derivarla. */
  readonly joinTable: string | null;
}

/** Superclase con `@MappedSuperclass` (espejo de `Superclass`). */
export interface Superclass {
  /** Id interno estable; lo crea la pantalla. */
  readonly id: string;
  /** Nombre en PascalCase. */
  readonly name: string;
  /** Id interno de la superclase padre, o `null` si es raíz. */
  readonly parentId: string | null;
  /** Atributos que aporta. */
  readonly attributes: readonly Attribute[];
}

/** Entidad del modelo (espejo de `Entity`). */
export interface Entity {
  /** Id interno estable; lo crea la pantalla. */
  readonly id: string;
  /** Nombre en PascalCase. */
  readonly name: string;
  /** Tabla escrita por el usuario, o `null` para derivarla. */
  readonly tableName: string | null;
  /** Ruta REST escrita por el usuario, o `null` para derivarla. */
  readonly resourcePath: string | null;
  /** Secuencia escrita por el usuario, o `null` para derivarla. */
  readonly sequenceName: string | null;
  /** Id interno de la superclase de la que hereda, o `null`. */
  readonly superclassId: string | null;
  /** Atributos propios. */
  readonly attributes: readonly Attribute[];
  /** Relaciones que declara. */
  readonly relations: readonly Relation[];
}

/** Campos de auditoría; van donde va el id (espejo de `AuditFields`). */
export interface AuditFields {
  /** Si se agrega la fecha de creación. */
  readonly createdAt: boolean;
  /** Si se agrega la fecha de última modificación. */
  readonly updatedAt: boolean;
}

/** Id de todas las entidades (espejo de `IdSettings`). */
export interface IdSettings {
  /** Tipo del id. */
  readonly type: ScalarType;
  /** Cómo se genera. */
  readonly generation: IdGeneration;
  /** Dónde se declaran el id y la auditoría. */
  readonly placement: IdPlacement;
}

/** Modelo de dominio completo (espejo de `DomainModel`). */
export interface DomainModel {
  /** Tipo, generación y ubicación del id. */
  readonly idSettings: IdSettings;
  /** Campos de auditoría. */
  readonly audit: AuditFields;
  /** Superclases, en el orden en que se muestran. */
  readonly superclasses: readonly Superclass[];
  /** Entidades, en el orden en que se muestran. */
  readonly entities: readonly Entity[];
}

/** Modelo vacío con los valores por defecto del backend. */
export const EMPTY_DOMAIN_MODEL: DomainModel = {
  idSettings: { type: 'LONG', generation: 'IDENTITY', placement: 'ENTITY' },
  audit: { createdAt: false, updatedAt: false },
  superclasses: [],
  entities: [],
};

/** Gravedad de un problema del modelo (espejo de `IssueSeverity`). */
export type IssueSeverity = 'ERROR' | 'WARNING';

/** Problema del modelo (espejo de `ModelIssue`). */
export interface ModelIssue {
  /** Error impide generar; aviso no. */
  readonly severity: IssueSeverity;
  /** Id interno del elemento afectado, o `null` si es del modelo en general. */
  readonly elementId: string | null;
  /** Ubicación legible, por ejemplo `Pedido › cliente`. */
  readonly location: string;
  /** Explicación. */
  readonly message: string;
}

/** Lado inverso de una relación bidireccional (espejo de `InverseRelation`). */
export interface InverseRelation {
  /** Nombre del inverso en la entidad destino. */
  readonly name: string;
  /** Id interno de la entidad que declara la relación. */
  readonly sourceEntityId: string;
  /** Id interno de la relación. */
  readonly relationId: string;
  /** Cardinalidad vista desde el destino. */
  readonly cardinality: Cardinality;
}

/** Mapeo relacional de una relación (espejo de `RelationMapping`). */
export interface RelationMapping {
  /** Id interno de la relación. */
  readonly relationId: string;
  /** Id interno de la entidad cuya tabla tiene la clave foránea o es dueña de la tabla intermedia. */
  readonly ownerEntityId: string;
  /** Columna de la clave foránea efectiva. */
  readonly joinColumn: string;
  /** Tabla intermedia efectiva, o `null` si no es N‑N. */
  readonly joinTable: string | null;
  /** Columna del otro lado en la tabla intermedia, o `null` si no es N‑N. */
  readonly inverseJoinColumn: string | null;
}

/** Nombres efectivos de una entidad (espejo de `EntityNamesResponse`). */
export interface EntityNames {
  /** Id interno de la entidad. */
  readonly entityId: string;
  /** Tabla efectiva. */
  readonly tableName: string;
  /** Ruta REST efectiva. */
  readonly resourcePath: string;
  /** Secuencia efectiva. */
  readonly sequenceName: string;
  /** Columna efectiva por id interno de atributo. */
  readonly columns: Readonly<Record<string, string>>;
  /** Inversos que llegan a esta entidad. */
  readonly inverses: readonly InverseRelation[];
}

/** Nombres efectivos de una superclase (espejo de `SuperclassNamesResponse`). */
export interface SuperclassNames {
  /** Id interno de la superclase. */
  readonly superclassId: string;
  /** Columna efectiva por id interno de atributo. */
  readonly columns: Readonly<Record<string, string>>;
}

/** Respuesta de `POST /api/v1/models/validate` (espejo de `ModelValidationResponse`). */
export interface ModelValidationResult {
  /** `true` si no hay errores; los avisos no cuentan. */
  readonly valid: boolean;
  /** Problemas, en el orden en que se encontraron. */
  readonly issues: readonly ModelIssue[];
  /** Nombres efectivos de cada entidad. */
  readonly entities: readonly EntityNames[];
  /** Nombres efectivos de cada superclase. */
  readonly superclasses: readonly SuperclassNames[];
  /** Mapeo de cada relación cuyo destino existe. */
  readonly relations: readonly RelationMapping[];
}

/** Grupos del catálogo que el modelo agrega solos (espejo de `ModelDependencyGroups`). */
export interface ModelDependencyGroups {
  /** Grupo de persistencia: entra si hay entidades y base de desarrollo. */
  readonly persistenceGroupId: string;
  /** Grupo de validación: entra si algo es obligatorio o tiene restricciones. */
  readonly validationGroupId: string;
}