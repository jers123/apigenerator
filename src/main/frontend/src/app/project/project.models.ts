import { BuildTool, DependencyKind, GradleScope, MavenScope } from "../catalog/catalog.models";
import { DomainModel } from "../model/model.models";

/** Estilo de arquitectura del proyecto generado. */
export type ArchitectureStyle =
  | 'LAYERED'
  | 'PACKAGE_BY_FEATURE'
  | 'HEXAGONAL'
  | 'ONION'
  | 'CLEAN'
  | 'VERTICAL_SLICE'
  | 'MODULITH';

/** Reparto del proyecto en uno o varios módulos de build. */
export type ModuleLayout = 'SINGLE_MODULE' | 'MULTI_MODULE';

/** Relación entre un patrón y un estilo. */
export type Compatibility = 'NATIVE' | 'COMPATIBLE' | 'CONTRAINDICATED' | 'NOT_APPLICABLE';

/** Catálogo de origen de un patrón. */
export type PatternFamily = 'CREATIONAL' | 'STRUCTURAL' | 'BEHAVIORAL' | 'ENTERPRISE' | 'ARCHITECTURAL' | 'DISTRIBUTED';

/** Id de un patrón, como `CQRS`; la lista completa llega del servidor. */
export type DesignPatternId = string;

/** Formato de empaquetado. */
export type Packaging = 'JAR' | 'WAR';

/** Formato de los archivos de configuración. */
export type ConfigFormat = 'PROPERTIES' | 'YAML';

/** Dónde guarda los datos el proyecto, en ejecución o en pruebas. */
export type Persistence = 'NONE' | 'IN_MEMORY' | 'DATABASE';

/** Tipo de emisor de tokens. */
export type IssuerType = 'OWN_SERVER' | 'PROVIDER';

/** Estilo del catálogo de arquitecturas. */
export interface ArchitectureStyleInfo {
  /** Identificador. */
  readonly id: ArchitectureStyle;
  /** Nombre legible. */
  readonly label: string;
  /** Explicación breve. */
  readonly description: string;
  /** Si admite varios módulos de build. */
  readonly multiModule: boolean;
}

/** Reparto en módulos del catálogo de arquitecturas. */
export interface ModuleLayoutInfo {
  /** Identificador. */
  readonly id: ModuleLayout;
  /** Nombre legible. */
  readonly label: string;
}

/** Patrón de diseño con su compatibilidad por estilo. */
export interface DesignPatternInfo {
  /** Identificador. */
  readonly id: DesignPatternId;
  /** Nombre legible. */
  readonly label: string;
  /** Catálogo de origen. */
  readonly family: PatternFamily;
  /** Nombre legible del catálogo de origen. */
  readonly familyLabel: string;
  /** `false` si solo se documenta en el README y el ADR. */
  readonly generatesCode: boolean;
  /** Por qué no se puede elegir en algunos estilos, o `null`. */
  readonly reason: string | null;
  /** Relación con cada estilo. */
  readonly compatibility: Readonly<Record<ArchitectureStyle, Compatibility>>;
}

/** Respuesta de `GET /api/v1/architectures`. */
export interface ArchitectureCatalog {
  /** Estilos, en el orden en que se muestran. */
  readonly styles: readonly ArchitectureStyleInfo[];
  /** Repartos en módulos. */
  readonly layouts: readonly ModuleLayoutInfo[];
  /** Todos los patrones, en el orden del catálogo. */
  readonly patterns: readonly DesignPatternInfo[];
}

/** Identidad del proyecto en la petición. */
export interface ProjectMetadataRequest {
  /** Organización, en minúsculas separadas por puntos. */
  readonly groupId: string;
  /** Nombre técnico, en minúsculas con guiones. */
  readonly artifactId: string;
  /** Nombre legible. */
  readonly name: string;
  /** Descripción; puede ir vacía. */
  readonly description: string;
}

/** Opciones de construcción en la petición. */
export interface BuildSettingsRequest {
  /** Herramienta de build. */
  readonly buildTool: BuildTool;
  /** Versión de Java. */
  readonly javaVersion: number;
  /** Formato de empaquetado. */
  readonly packaging: Packaging;
  /** Si usa la pila reactiva. */
  readonly reactive: boolean;
  /** Puerto HTTP de la aplicación generada. */
  readonly serverPort: number;
}

/** Arquitectura en la petición. */
export interface ArchitectureRequest {
  /** Estilo de arquitectura. */
  readonly style: ArchitectureStyle;
  /** Reparto en módulos. */
  readonly layout: ModuleLayout;
  /** Patrones opcionales elegidos; los nativos los agrega el servidor. */
  readonly patterns: readonly DesignPatternId[];
}

/** Extras de la generación. */
export interface GenerationOptions {
  /** Javadoc en español en cada clase, atributo y método. */
  readonly javadoc: boolean;
  /** Pruebas unitarias y, con base de datos, de integración. */
  readonly unitTests: boolean;
  /** Formato de los archivos de configuración. */
  readonly configFormat: ConfigFormat;
}

/** Conexión a base de datos en la petición. */
export interface DatabaseConnectionRequest {
  /** Id del motor. */
  readonly engineId: string;
  /** Servidor; se ignora en bases embebidas. */
  readonly host: string | null;
  /** Puerto; se ignora en bases embebidas. */
  readonly port: number | null;
  /** Nombre de la base. */
  readonly databaseName: string;
  /** Usuario. */
  readonly username: string;
  /** Contraseña; puede ir vacía. */
  readonly password: string;
}

/** Emisor de tokens en la petición. */
export interface IssuerRequest {
  /** Servidor propio o proveedor externo. */
  readonly type: IssuerType;
  /** Id del proveedor; `null` con el servidor propio. */
  readonly providerId: string | null;
  /** Datos del proveedor por clave; vacío con el servidor propio. */
  readonly values: Readonly<Record<string, string>>;
}

/** Seguridad en la petición. */
export interface SecurityRequest {
  /** Método de autenticación de la API, o `null` si es pública. */
  readonly apiAuthenticationId: string | null;
  /** Método de inicio de sesión, o `null`. */
  readonly browserLoginId: string | null;
  /** Complementos elegidos. */
  readonly addOnIds: readonly string[];
  /** Emisor de tokens, o `null` si ningún método lo usa. */
  readonly issuer: IssuerRequest | null;
}

/** Cuerpo de `POST /api/v1/projects/plan`. */
export interface ProjectRequest {
  /** Identidad del proyecto. */
  readonly metadata: ProjectMetadataRequest;
  /** Opciones de construcción. */
  readonly build: BuildSettingsRequest;
  /** Estilo, reparto y patrones. */
  readonly architecture: ArchitectureRequest;
  /** Javadoc, pruebas y formato de configuración. */
  readonly options: GenerationOptions;
  /** Ids de los grupos marcados (sin los de base de datos ni los del modelo). */
  readonly dependencyGroupIds: readonly string[];
  /** Base de desarrollo, o `null` para trabajar en memoria. */
  readonly database: DatabaseConnectionRequest | null;
  /** Base de pruebas, o `null` para probar en memoria. */
  readonly testDatabase: DatabaseConnectionRequest | null;
  /** Versiones elegidas por clave `groupId:artifactId`; solo lo que cambió el usuario. */
  readonly versions: Readonly<Record<string, string>>;
  /** Seguridad elegida. */
  readonly security: SecurityRequest;
  /** Modelo de dominio. */
  readonly model: DomainModel;
}

/** Dependencia del plan con su versión final. */
export interface PlannedDependency {
  /** Nombre legible. */
  readonly name: string;
  /** groupId de Maven. */
  readonly groupId: string;
  /** artifactId de Maven. */
  readonly artifactId: string;
  /** Tipo de artefacto. */
  readonly kind: DependencyKind;
  /** Ámbito en Maven, o `null` si no es librería. */
  readonly mavenScope: MavenScope | null;
  /** Configuración en Gradle, o `null` si no es librería. */
  readonly gradleScope: GradleScope | null;
  /** Versión que se escribirá, o `null` si es gestionada. */
  readonly version: string | null;
  /** Si la versión la gestiona Spring Boot o un BOM. */
  readonly managed: boolean;
}

/** Conexión del plan, sin contraseña. */
export interface ConnectionSummary {
  /** Id del motor. */
  readonly engineId: string;
  /** URL JDBC final. */
  readonly jdbcUrl: string;
  /** Usuario. */
  readonly username: string;
}

/** Seguridad del plan (espejo de `SecurityPlanResponse`). */
export interface SecuritySummary {
  /** Nombre de la autenticación de la API, o `null`. */
  readonly apiAuthentication: string | null;
  /** Nombre del inicio de sesión, o `null`. */
  readonly browserLogin: string | null;
  /** Nombres de los complementos. */
  readonly addOns: readonly string[];
  /** Tipo de emisor, o `null`. */
  readonly issuerType: IssuerType | null;
  /** Nombre del proveedor, o `null`. */
  readonly providerName: string | null;
  /** URL del emisor, o `null`. */
  readonly issuerUri: string | null;
  /** Si el proyecto guarda usuarios. */
  readonly managesUsers: boolean;
  /** artifactId del segundo proyecto, o `null`. */
  readonly authorizationServerArtifactId: string | null;
  /** Puerto del segundo proyecto, o `null`. */
  readonly authorizationServerPort: number | null;
}

/** Respuesta de `POST /api/v1/projects/plan`. */
export interface ProjectPlan {
  /** groupId del proyecto. */
  readonly groupId: string;
  /** artifactId del proyecto. */
  readonly artifactId: string;
  /** Paquete raíz del código generado. */
  readonly basePackage: string;
  /** Versión de Spring Boot. */
  readonly springBootVersion: string;
  /** Arquitectura con los patrones nativos incluidos. */
  readonly architecture: ArchitectureRequest;
  /** Opciones de generación. */
  readonly options: GenerationOptions;
  /** Dónde guarda los datos al ejecutarse. */
  readonly persistence: Persistence;
  /** Dónde guardan los datos las pruebas. */
  readonly testPersistence: Persistence;
  /** Dependencias finales. */
  readonly dependencies: readonly PlannedDependency[];
  /** Base de desarrollo, o `null`. */
  readonly database: ConnectionSummary | null;
  /** Base de pruebas, o `null`. */
  readonly testDatabase: ConnectionSummary | null;
  /** Seguridad y segundo proyecto. */
  readonly security: SecuritySummary;
}