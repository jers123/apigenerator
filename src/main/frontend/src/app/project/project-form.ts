import { FormControl, FormGroup, FormRecord, Validators } from "@angular/forms";
import { ArchitectureRequest, ArchitectureStyle, BuildSettingsRequest, ConfigFormat, DatabaseConnectionRequest, DesignPatternId, IssuerRequest, IssuerType, ModuleLayout, Packaging, ProjectMetadataRequest, ProjectRequest, SecurityRequest } from "./project.models";
import { BuildTool } from "../catalog/catalog.models";
import { notBlank, SLUG_PATTERN } from "../catalog/group-editor/group-form";
import { DatabaseEngine } from "../database/database.models";
import { modelForm, ModelForm, toDomainModel } from "../model/model-form";
import { AttributeConstraints, DomainModel, ModelDependencyGroups } from "../model/model.models";
import { IdentityProviderDefinition } from "../security/security.models";

/** Versiones LTS de Java que admite Spring Boot 4. */
export const JAVA_VERSIONS: readonly number[] = [17, 21, 25, 27];

/** Mayor número de puerto TCP válido. */
const MAX_PORT = 65_535;

/** Paquete Java en minúsculas; igual que en `ProjectMetadata` del backend. */
const GROUP_ID_PATTERN = /^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*$/;

/** Nombre de base válido en las URL JDBC; igual que en `DatabaseConnection` del backend. */
const DATABASE_NAME_PATTERN = /^[A-Za-z][A-Za-z0-9_]*$/;

/** Paso "Proyecto": identidad y opciones de construcción. */
export type BasicsForm = FormGroup<{
  /** Organización, en minúsculas separadas por puntos. */
  groupId: FormControl<string>;
  /** Nombre técnico, en minúsculas con guiones. */
  artifactId: FormControl<string>;
  /** Nombre legible. */
  name: FormControl<string>;
  /** Descripción opcional. */
  description: FormControl<string>;
  /** Herramienta de build. */
  buildTool: FormControl<BuildTool>;
  /** Versión de Java. */
  javaVersion: FormControl<number>;
  /** Formato de empaquetado. */
  packaging: FormControl<Packaging>;
  /** Si usa la pila reactiva. */
  reactive: FormControl<boolean>;
  /** Puerto HTTP; `null` mientras el campo está vacío. */
  serverPort: FormControl<number | null>;
}>;

/** Paso "Arquitectura". */
export type ArchitectureForm = FormGroup<{
  /** Estilo de arquitectura. */
  style: FormControl<ArchitectureStyle>;
  /** Reparto en módulos. */
  layout: FormControl<ModuleLayout>;
  /** Patrones opcionales elegidos; los nativos no se guardan aquí. */
  patterns: FormControl<readonly DesignPatternId[]>;
}>;

/** Conexión a una base de datos. Deshabilitada cuando esa base no se usa. */
export type ConnectionForm = FormGroup<{
  /** Id del motor elegido; vacío hasta elegir uno. */
  engineId: FormControl<string>;
  /** Grupo del catálogo con el driver del motor elegido; interno, no se envía. */
  driverGroupId: FormControl<string>;
  /** Servidor; deshabilitado en motores embebidos. */
  host: FormControl<string>;
  /** Puerto; deshabilitado en motores embebidos. */
  port: FormControl<number | null>;
  /** Nombre de la base. */
  databaseName: FormControl<string>;
  /** Usuario. */
  username: FormControl<string>;
  /** Contraseña; puede ir vacía. */
  password: FormControl<string>;
}>;

/** Paso "Bases de datos". */
export type DatabasesForm = FormGroup<{
  /** Si el proyecto usa base de datos de desarrollo. */
  useDatabase: FormControl<boolean>;
  /** Conexión de desarrollo. */
  database: ConnectionForm;
  /** Si las pruebas usan su propia base de datos. */
  useTestDatabase: FormControl<boolean>;
  /** Conexión de pruebas. */
  testDatabase: ConnectionForm;
}>;

/** Emisor de tokens. Deshabilitado mientras ningún método elegido lo necesite. */
export type IssuerForm = FormGroup<{
  /** Servidor propio o proveedor; `null` hasta elegir. */
  type: FormControl<IssuerType | null>;
  /** Proveedor elegido; habilitado solo con `PROVIDER`. */
  providerId: FormControl<string>;
  /** Datos del proveedor, un control por parámetro; habilitado solo con `PROVIDER`. */
  values: FormRecord<FormControl<string>>;
}>;

/** Paso "Seguridad". */
export type SecurityForm = FormGroup<{
  /** Método de autenticación de la API, o `null` si es pública. */
  apiAuthenticationId: FormControl<string | null>;
  /** Método de inicio de sesión, o `null`. */
  browserLoginId: FormControl<string | null>;
  /** Complementos elegidos. */
  addOnIds: FormControl<readonly string[]>;
  /** Emisor de tokens. */
  issuer: IssuerForm;
}>;

/** Paso "Dependencias". */
export type DependenciesForm = FormGroup<{
  /** Grupos marcados por el usuario, sin los de base de datos, de build ni del modelo. */
  groupIds: FormControl<readonly string[]>;
  /** Versiones que cambió el usuario, por clave `groupId:artifactId`. */
  versions: FormControl<Readonly<Record<string, string>>>;
}>;

/** Paso "Opciones". */
export type OptionsForm = FormGroup<{
  /** Javadoc en español en cada clase, atributo y método. */
  javadoc: FormControl<boolean>;
  /** Pruebas unitarias y, con base de datos, de integración. */
  unitTests: FormControl<boolean>;
  /** Formato de los archivos de configuración. */
  configFormat: FormControl<ConfigFormat>;
}>;

/** Formulario completo del asistente. */
export type ProjectForm = FormGroup<{
  /** Paso "Proyecto". */
  basics: BasicsForm;
  /** Paso "Arquitectura". */
  architecture: ArchitectureForm;
  /** Paso "Bases de datos". */
  databases: DatabasesForm;
  /** Paso "Seguridad". */
  security: SecurityForm;
  /** Paso "Modelo". */
  model: ModelForm;
  /** Paso "Dependencias". */
  dependencies: DependenciesForm;
  /** Paso "Opciones". */
  options: OptionsForm;
}>;

/**
 * Crea el formulario del asistente con valores iniciales válidos.
 * @returns formulario listo para editar
 */
export function projectForm(): ProjectForm {
  return new FormGroup({
    basics: new FormGroup({
      groupId: new FormControl('com.example', {
        nonNullable: true,
        validators: [notBlank, Validators.pattern(GROUP_ID_PATTERN)],
      }),
      artifactId: new FormControl('demo', { nonNullable: true, validators: [notBlank, Validators.pattern(SLUG_PATTERN)] }),
      name: new FormControl('Demo', { nonNullable: true, validators: notBlank }),
      description: new FormControl('', { nonNullable: true }),
      buildTool: new FormControl<BuildTool>('MAVEN', { nonNullable: true }),
      javaVersion: new FormControl(25, { nonNullable: true }),
      packaging: new FormControl<Packaging>('JAR', { nonNullable: true }),
      reactive: new FormControl(false, { nonNullable: true }),
      serverPort: new FormControl<number | null>(8080, {
        validators: [Validators.required, Validators.min(1), Validators.max(MAX_PORT)],
      }),
    }),
    architecture: new FormGroup({
      style: new FormControl<ArchitectureStyle>('LAYERED', { nonNullable: true }),
      layout: new FormControl<ModuleLayout>('SINGLE_MODULE', { nonNullable: true }),
      patterns: new FormControl<readonly DesignPatternId[]>([], { nonNullable: true }),
    }),
    databases: new FormGroup({
      useDatabase: new FormControl(false, { nonNullable: true }),
      database: connectionForm('app'),
      useTestDatabase: new FormControl(false, { nonNullable: true }),
      testDatabase: connectionForm('app_test'),
    }),
    security: new FormGroup({
      apiAuthenticationId: new FormControl<string | null>(null),
      browserLoginId: new FormControl<string | null>(null),
      addOnIds: new FormControl<readonly string[]>([], { nonNullable: true }),
      issuer: issuerForm(),
    }),
    model: modelForm(),
    dependencies: new FormGroup({
      groupIds: new FormControl<readonly string[]>([], { nonNullable: true }),
      versions: new FormControl<Readonly<Record<string, string>>>({}, { nonNullable: true }),
    }),
    options: new FormGroup({
      javadoc: new FormControl(true, { nonNullable: true }),
      unitTests: new FormControl(true, { nonNullable: true }),
      configFormat: new FormControl<ConfigFormat>('YAML', { nonNullable: true }),
    }),
  });
}

/**
 * Paquete raíz que tendrá el código, igual que lo calcula el backend.
 * @param basics paso "Proyecto"
 * @returns `groupId` + `artifactId` sin guiones, o `null` si alguno no es válido
 */
export function basePackageOf(basics: BasicsForm): string | null {
  const { groupId, artifactId } = basics.controls;
  return groupId.valid && artifactId.valid
    ? `${groupId.value.trim()}.${artifactId.value.trim().replaceAll('-', '')}`
    : null;
}

/**
 * Identidad del proyecto para la API.
 * @param basics paso "Proyecto"
 * @returns metadatos con los textos recortados
 */
export function toMetadataRequest(basics: BasicsForm): ProjectMetadataRequest {
  const value = basics.getRawValue();
  return {
    groupId: value.groupId.trim(),
    artifactId: value.artifactId.trim(),
    name: value.name.trim(),
    description: value.description.trim(),
  };
}

/**
 * Opciones de construcción para la API.
 * @param basics paso "Proyecto"; debe ser válido
 * @returns opciones de construcción
 */
export function toBuildRequest(basics: BasicsForm): BuildSettingsRequest {
  const value = basics.getRawValue();
  return {
    buildTool: value.buildTool,
    javaVersion: value.javaVersion,
    packaging: value.packaging,
    reactive: value.reactive,
    serverPort: value.serverPort ?? 0,
  };
}

/**
 * Arquitectura para la API.
 * @param architecture paso "Arquitectura"
 * @returns estilo, reparto y patrones opcionales elegidos
 */
export function toArchitectureRequest(architecture: ArchitectureForm): ArchitectureRequest {
  const value = architecture.getRawValue();
  return { style: value.style, layout: value.layout, patterns: [...value.patterns] };
}

/**
 * Petición completa para `POST /api/v1/projects/plan` con lo elegido en todos
 * los pasos. Si hay base de pruebas, pide pruebas aunque la opción esté
 * apagada, porque esa base la usan las pruebas de integración.
 * @param form formulario del asistente
 * @returns petición lista para enviar
 */
export function toProjectRequest(form: ProjectForm): ProjectRequest {
  const databases = form.controls.databases.controls;
  const testDatabase = toConnectionRequest(databases.testDatabase);
  const options = form.controls.options.getRawValue();
  const dependencies = form.controls.dependencies.getRawValue();
  return {
    metadata: toMetadataRequest(form.controls.basics),
    build: toBuildRequest(form.controls.basics),
    architecture: toArchitectureRequest(form.controls.architecture),
    options: {
      javadoc: options.javadoc,
      unitTests: options.unitTests || testDatabase !== null,
      configFormat: options.configFormat,
    },
    dependencyGroupIds: [...dependencies.groupIds],
    database: toConnectionRequest(databases.database),
    testDatabase,
    versions: { ...dependencies.versions },
    security: toSecurityRequest(form.controls.security),
    model: toDomainModel(form.controls.model),
  };
}

/**
 * Indica si un atributo tiene alguna restricción; igual que
 * `AttributeConstraints.hasAny` del backend.
 * @param constraints restricciones del atributo
 * @returns `true` si alguna está definida
 */
function hasAnyConstraint(constraints: AttributeConstraints): boolean {
  return (
    constraints.minLength !== null ||
    constraints.maxLength !== null ||
    constraints.min !== null ||
    constraints.max !== null ||
    constraints.integerDigits !== null ||
    constraints.fractionDigits !== null ||
    constraints.pattern !== null ||
    constraints.email ||
    constraints.temporal !== null
  );
}

/**
 * Grupos del catálogo que el modelo agrega solo; misma regla que
 * `ProjectPlanner.groupsRequiredByModel` del backend.
 * @param model modelo del paso "Modelo"
 * @param hasDatabase si hay base de datos de desarrollo
 * @param groups ids configurados en el servidor, o `null` si aún no llegan
 * @returns persistencia si hay entidades y base; validación si algo es
 *   obligatorio o tiene restricciones
 */
export function modelDependencyGroupIds(
  model: DomainModel,
  hasDatabase: boolean,
  groups: ModelDependencyGroups | null,
): readonly string[] {
  if (!groups) {
    return [];
  }
  const ids: string[] = [];
  if (model.entities.length > 0 && hasDatabase) {
    ids.push(groups.persistenceGroupId);
  }
  const attributes = [
    ...model.superclasses.flatMap((superclass) => superclass.attributes),
    ...model.entities.flatMap((entity) => entity.attributes),
  ];
  const needsValidation =
    attributes.some((attribute) => attribute.required || hasAnyConstraint(attribute.constraints)) ||
    model.entities.some((entity) => entity.relations.some((relation) => relation.required));
  if (needsValidation) {
    ids.push(groups.validationGroupId);
  }
  return ids;
}

/**
 * Nombres de los motores elegidos (desarrollo y pruebas) que no tienen
 * secuencias, para avisar en el paso "Modelo".
 * @param databases paso "Bases de datos"
 * @param engines motores registrados
 * @returns nombres sin repetir, en el orden de los motores
 */
export function enginesWithoutSequences(
  databases: DatabasesForm,
  engines: readonly DatabaseEngine[],
): readonly string[] {
  const chosen = [databases.controls.database, databases.controls.testDatabase]
    .filter((connection) => connection.enabled)
    .map((connection) => connection.controls.engineId.value);
  const labels = engines
    .filter((engine) => chosen.includes(engine.id) && !engine.supportsSequences)
    .map((engine) => engine.label);
  return [...new Set(labels)];
}

/**
 * Crea una conexión vacía y deshabilitada, porque al inicio no hay base de datos.
 * @param databaseName nombre de base que se propone
 * @returns conexión con las reglas del backend
 */
function connectionForm(databaseName: string): ConnectionForm {
  const form: ConnectionForm = new FormGroup({
    engineId: new FormControl('', { nonNullable: true, validators: notBlank }),
    driverGroupId: new FormControl('', { nonNullable: true }),
    host: new FormControl('localhost', { nonNullable: true, validators: notBlank }),
    port: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(1), Validators.max(MAX_PORT)],
    }),
    databaseName: new FormControl(databaseName, {
      nonNullable: true,
      validators: [notBlank, Validators.pattern(DATABASE_NAME_PATTERN)],
    }),
    username: new FormControl('', { nonNullable: true, validators: notBlank }),
    password: new FormControl('', { nonNullable: true }),
  });
  form.disable();
  return form;
}

/**
 * Elige un motor para una conexión habilitada: copia su puerto y usuario por
 * defecto, y deshabilita servidor y puerto si es embebido.
 * @param connection conexión habilitada
 * @param engine motor elegido
 */
export function applyEngine(connection: ConnectionForm, engine: DatabaseEngine): void {
  const { engineId, driverGroupId, host, port, username } = connection.controls;
  engineId.setValue(engine.id);
  driverGroupId.setValue(engine.driverGroupId);
  port.setValue(engine.defaultPort);
  username.setValue(engine.defaultUsername);
  if (engine.embedded) {
    host.disable();
    port.disable();
  } else {
    host.enable();
    port.enable();
  }
}

/**
 * Activa o desactiva una conexión. Al activarla, conserva lo escrito y vuelve
 * a ocultar servidor y puerto si su motor es embebido.
 * @param connection conexión
 * @param enabled si la base se usa
 * @param engine motor ya elegido, o `undefined`
 */
export function setConnectionEnabled(
  connection: ConnectionForm,
  enabled: boolean,
  engine: DatabaseEngine | undefined,
): void {
  if (!enabled) {
    connection.disable();
    return;
  }
  connection.enable();
  if (engine?.embedded) {
    connection.controls.host.disable();
    connection.controls.port.disable();
  }
}

/**
 * Conexión para la API.
 * @param connection conexión del formulario
 * @returns la conexión, o `null` si está desactivada; servidor y puerto van
 *   en `null` si el motor es embebido
 */
export function toConnectionRequest(connection: ConnectionForm): DatabaseConnectionRequest | null {
  if (connection.disabled) {
    return null;
  }
  const value = connection.getRawValue();
  const server = connection.controls.host.enabled;
  return {
    engineId: value.engineId,
    host: server ? value.host.trim() : null,
    port: server ? value.port : null,
    databaseName: value.databaseName.trim(),
    username: value.username.trim(),
    password: value.password,
  };
}

/**
 * URL JDBC que resultará, para revisarla antes de generar.
 * @param connection conexión del formulario
 * @param engine motor elegido, o `undefined`
 * @returns la URL con los valores escritos, o `null` si no hay motor
 */
export function jdbcUrlPreview(connection: ConnectionForm, engine: DatabaseEngine | undefined): string | null {
  if (!engine) {
    return null;
  }
  const value = connection.getRawValue();
  return engine.jdbcUrlTemplate
    .replaceAll('{host}', value.host.trim() || 'localhost')
    .replaceAll('{port}', String(value.port ?? ''))
    .replaceAll('{database}', value.databaseName.trim());
}

/**
 * Crea el emisor vacío y deshabilitado, porque al inicio la API es pública.
 * @returns emisor con sus reglas
 */
function issuerForm(): IssuerForm {
  const form: IssuerForm = new FormGroup({
    type: new FormControl<IssuerType | null>(null, { validators: Validators.required }),
    providerId: new FormControl('', { nonNullable: true, validators: notBlank }),
    values: new FormRecord<FormControl<string>>({}),
  });
  form.disable();
  return form;
}

/**
 * Habilita los datos del proveedor solo si el tipo elegido es `PROVIDER`.
 * @param issuer emisor habilitado
 */
function syncProviderControls(issuer: IssuerForm): void {
  const { providerId, values } = issuer.controls;
  if (issuer.controls.type.value === 'PROVIDER') {
    providerId.enable();
    values.enable();
  } else {
    providerId.disable();
    values.disable();
  }
}

/**
 * Activa o desactiva el emisor. Al activarlo conserva lo elegido.
 * @param issuer emisor
 * @param enabled si algún método elegido lo necesita
 */
export function setIssuerEnabled(issuer: IssuerForm, enabled: boolean): void {
  if (!enabled) {
    issuer.disable();
    return;
  }
  issuer.controls.type.enable();
  syncProviderControls(issuer);
}

/**
 * Elige el tipo de emisor.
 * @param issuer emisor habilitado
 * @param type servidor propio, proveedor o `null` para ninguno
 */
export function chooseIssuerType(issuer: IssuerForm, type: IssuerType | null): void {
  issuer.controls.type.setValue(type);
  syncProviderControls(issuer);
}

/**
 * Elige un proveedor: crea un campo obligatorio por cada uno de sus
 * parámetros y descarta los del proveedor anterior.
 * @param issuer emisor con tipo `PROVIDER`
 * @param provider proveedor elegido, o `null` para quitarlo
 */
export function applyProvider(issuer: IssuerForm, provider: IdentityProviderDefinition | null): void {
  issuer.controls.providerId.setValue(provider?.id ?? '');
  const values = issuer.controls.values;
  for (const key of Object.keys(values.controls)) {
    values.removeControl(key);
  }
  for (const parameter of provider?.parameters ?? []) {
    values.addControl(parameter.key, new FormControl('', { nonNullable: true, validators: notBlank }));
  }
}

/**
 * Seguridad para la API.
 * @param security paso "Seguridad"
 * @returns seguridad con el emisor solo si está habilitado y elegido
 */
export function toSecurityRequest(security: SecurityForm): SecurityRequest {
  const value = security.getRawValue();
  return {
    apiAuthenticationId: value.apiAuthenticationId,
    browserLoginId: value.browserLoginId,
    addOnIds: [...value.addOnIds],
    issuer: toIssuerRequest(security.controls.issuer),
  };
}

/**
 * Emisor para la API.
 * @param issuer emisor del formulario
 * @returns el emisor, o `null` si está deshabilitado o sin tipo; el
 *   servidor propio no lleva proveedor ni datos
 */
function toIssuerRequest(issuer: IssuerForm): IssuerRequest | null {
  const value = issuer.getRawValue();
  if (issuer.disabled || value.type === null) {
    return null;
  }
  if (value.type === 'OWN_SERVER') {
    return { type: 'OWN_SERVER', providerId: null, values: {} };
  }
  return {
    type: 'PROVIDER',
    providerId: value.providerId,
    values: Object.fromEntries(Object.entries(value.values).map(([key, text]) => [key, text.trim()])),
  };
}