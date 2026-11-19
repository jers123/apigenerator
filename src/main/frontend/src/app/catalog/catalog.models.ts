/** Tipo de artefacto; decide dónde se escribe en el archivo de build. */
export type DependencyKind = 'LIBRARY' | 'PLUGIN' | 'BOM' | 'PARENT' | 'BUILD_TOOL';

/** Ámbito de una librería en Maven. */
export type MavenScope = 'COMPILE' | 'IMPORT' | 'PROVIDED' | 'RUNTIME' | 'SYSTEM' | 'TEST';

/** Configuración de una librería en Gradle. */
export type GradleScope =
  | 'ANNOTATION_PROCESSOR'
  | 'API'
  | 'COMPILE_ONLY'
  | 'DEVELOPMENT_ONLY'
  | 'IMPLEMENTATION'
  | 'RUNTIME_ONLY'
  | 'TEST_COMPILE_ONLY'
  | 'TEST_IMPLEMENTATION'
  | 'TEST_RUNTIME_ONLY';

/** Categoría de un grupo de dependencias. */
export type DependencyCategory = 'MAVEN' | 'GRADLE' | 'SPRING' | 'DATABASE' | 'OTHER';

/** Herramienta de build del proyecto generado. */
export type BuildTool = 'MAVEN' | 'GRADLE';

/** Dependencia del catálogo (espejo de `DependencyResponse`). */
export interface Dependency {
  /** Nombre legible. */
  readonly name: string;
  /** groupId de Maven. */
  readonly groupId: string;
  /** artifactId de Maven. */
  readonly artifactId: string;
  /** Versión explícita, o `null` si la gestiona el parent o un BOM. */
  readonly version: string | null;
  /** Tipo de artefacto. */
  readonly kind: DependencyKind;
  /** Ámbito en Maven, o `null` si no es una librería. */
  readonly mavenScope: MavenScope | null;
  /** Configuración en Gradle, o `null` si no es una librería. */
  readonly gradleScope: GradleScope | null;
  /** Si se incluye siempre que su grupo está activo. */
  readonly includedByDefault: boolean;
  /** Si pertenece a la pila reactiva. */
  readonly reactive: boolean;
  /** Si su versión la fija el parent de Spring Boot; esas no se eligen por separado. */
  readonly followsSpringBoot: boolean;
}

/** Grupo de dependencias (espejo de `GroupResponse`). */
export interface DependencyGroup {
  /** Identificador estable; no cambia aunque cambie el nombre. */
  readonly id: string;
  /** Nombre visible en la interfaz; se puede renombrar. */
  readonly name: string;
  /** Categoría del grupo. */
  readonly category: DependencyCategory;
  /** Nombre legible de la categoría. */
  readonly categoryLabel: string;
  /** Si aparece marcado al abrir la pantalla. */
  readonly selectedByDefault: boolean;
  /** Si necesita una base de datos de desarrollo; sin ella, el grupo no se puede elegir. */
  readonly requiresDatabase: boolean;
  /** Dependencias que aporta en un proyecto no reactivo (calculadas por el servidor). */
  readonly standardDependencies: readonly Dependency[];
  /** Dependencias que aporta en un proyecto reactivo (calculadas por el servidor). */
  readonly reactiveDependencies: readonly Dependency[];
}

/** Respuesta de `GET /api/v1/catalog`. */
export interface Catalog {
  /** Grupos en el orden en que deben mostrarse. */
  readonly groups: readonly DependencyGroup[];
}

/** Cuerpo de `POST /api/v1/catalog/resolve`. */
export interface ResolveRequest {
  /** Ids de los grupos marcados. */
  readonly groupIds: readonly string[];
  /** Herramienta de build del proyecto. */
  readonly buildTool: BuildTool;
  /** Si el proyecto usa la pila reactiva. */
  readonly reactive: boolean;
}

/** Respuesta de `POST /api/v1/catalog/resolve`. */
export interface ResolveResponse {
  /** Ids de los grupos que quedaron activos. */
  readonly groupIds: readonly string[];
  /** Dependencias finales del proyecto. */
  readonly dependencies: readonly Dependency[];
}

/** Dependencia tal como se guarda en el catálogo (espejo de `DependencyDefinition`). */
export interface DependencyDefinition {
  /** Nombre legible. */
  readonly name: string;
  /** groupId de Maven. */
  readonly groupId: string;
  /** artifactId de Maven. */
  readonly artifactId: string;
  /** Versión explícita, o `null` si la gestiona Spring Boot o un BOM. */
  readonly version: string | null;
  /** Tipo de artefacto. */
  readonly kind: DependencyKind;
  /** Ámbito en Maven; obligatorio solo para librerías. */
  readonly mavenScope: MavenScope | null;
  /** Configuración en Gradle; obligatoria solo para librerías. */
  readonly gradleScope: GradleScope | null;
  /** Si se incluye siempre que su grupo está activo. */
  readonly includedByDefault: boolean;
  /** Si pertenece a la pila reactiva. */
  readonly reactive: boolean;
}

/**
 * Grupo tal como se guarda en el catálogo (espejo de `GroupDefinition`).
 * Es el cuerpo y la respuesta de `/api/v1/catalog/groups`.
 */
export interface GroupDefinition {
  /** Identificador estable; no se puede cambiar después de crearlo. */
  readonly id: string;
  /** Nombre visible; se puede cambiar. */
  readonly name: string;
  /** Categoría del grupo. */
  readonly category: DependencyCategory;
  /** Si aparece marcado al abrir la pantalla de selección. */
  readonly selectedByDefault: boolean;
  /** Si necesita una base de datos de desarrollo; sin ella, el grupo no se puede elegir. */
  readonly requiresDatabase: boolean;
  /** Artefactos del grupo, en orden. */
  readonly dependencies: readonly DependencyDefinition[];
}