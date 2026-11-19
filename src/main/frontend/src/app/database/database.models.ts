import { GroupDefinition } from "../catalog/catalog.models";

/** Tipo de base de datos de un motor (espejo de `DatabaseKind`). */
export type DatabaseKind = 'RELATIONAL' | 'DOCUMENT';

/** Opción de tipo de base para los formularios. */
export interface DatabaseKindOption {
  /** Valor que se envía a la API. */
  readonly value: DatabaseKind;
  /** Nombre legible. */
  readonly label: string;
  /** Si el generador ya lo soporta; si no, se muestra deshabilitado. */
  readonly supported: boolean;
}

/** Tipos de base en el orden en que se ofrecen; igual que en el backend. */
export const DATABASE_KINDS: readonly DatabaseKindOption[] = [
  { value: 'RELATIONAL', label: 'Relacional (SQL)', supported: true },
  { value: 'DOCUMENT', label: 'Documentos (NoSQL)', supported: false },
];

/** Motor de base de datos (espejo de `DatabaseEngineResponse`). */
export interface DatabaseEngine {
  /** Identificador estable. */
  readonly id: string;
  /** Nombre legible. */
  readonly label: string;
  /** Grupo del catálogo que aporta el driver. */
  readonly driverGroupId: string;
  /** Si corre dentro de la aplicación, sin servidor. */
  readonly embedded: boolean;
  /** Puerto por defecto, o `null` si es embebida. */
  readonly defaultPort: number | null;
  /** Usuario por defecto. */
  readonly defaultUsername: string;
  /** Plantilla con `{host}`, `{port}` y `{database}`. */
  readonly jdbcUrlTemplate: string;
  /** Tipo de base. */
  readonly kind: DatabaseKind;
  /** Si tiene secuencias, necesarias para la estrategia de id `SEQUENCE`. */
  readonly supportsSequences: boolean;
  /** URL de ejemplo calculada por el servidor, para revisar la plantilla. */
  readonly exampleUrl: string;
}

/** Cuerpo de `POST /api/v1/databases` y `PUT /api/v1/databases/{id}`. */
export interface DatabaseEngineRequest {
  /** Identificador; al editar debe coincidir con el de la URL. */
  readonly id: string;
  /** Nombre legible. */
  readonly label: string;
  /** Si corre dentro de la aplicación, sin servidor. */
  readonly embedded: boolean;
  /** Puerto por defecto; `null` si es embebida. */
  readonly defaultPort: number | null;
  /** Usuario por defecto. */
  readonly defaultUsername: string;
  /** Plantilla con `{host}`, `{port}` y `{database}`. */
  readonly jdbcUrlTemplate: string;
  /** Grupo de base de datos con el driver. */
  readonly driverGroup: GroupDefinition;
  /** Tipo de base. */
  readonly kind: DatabaseKind;
  /** Si tiene secuencias. */
  readonly supportsSequences: boolean;
}