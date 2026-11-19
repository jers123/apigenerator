/** Coordenadas Maven de un artefacto, sin versión. */
export interface ArtifactCoordinates {
  /** groupId del artefacto. */
  readonly groupId: string;
  /** artifactId del artefacto. */
  readonly artifactId: string;
}

/** Cuerpo de `POST /api/v1/versions`. */
export interface VersionLookupRequest {
  /** Artefactos a consultar; máximo 100. */
  readonly artifacts: readonly ArtifactCoordinates[];
}

/** Versiones de un artefacto (espejo de `ArtifactVersionsResponse`). */
export interface ArtifactVersions extends ArtifactCoordinates {
  /** Versión estable más reciente, o `null` si no se pudo consultar. */
  readonly latest: string | null;
  /** Versiones estables, de la más nueva a la más antigua. */
  readonly versions: readonly string[];
  /** `false` si Maven Central no respondió o el artefacto no existe. */
  readonly available: boolean;
}

/** Respuesta de `POST /api/v1/versions`. */
export interface VersionLookupResponse {
  /** Un resultado por artefacto consultado. */
  readonly artifacts: readonly ArtifactVersions[];
}

/**
 * Clave única de un artefacto, igual que en Maven.
 * @param artifact coordenadas del artefacto
 * @returns texto con el formato `groupId:artifactId`
 */
export function coordinatesKey(artifact: ArtifactCoordinates): string {
  return `${artifact.groupId}:${artifact.artifactId}`;
}

/**
 * Compara dos listas de artefactos por sus claves, para no repetir una
 * consulta de versiones cuando la lista no cambió.
 * @param a lista anterior
 * @param b lista nueva
 * @returns `true` si contienen los mismos artefactos en el mismo orden
 */
export function sameArtifacts(a: readonly ArtifactCoordinates[], b: readonly ArtifactCoordinates[]): boolean {
  return a.length === b.length && a.every((artifact, i) => coordinatesKey(artifact) === coordinatesKey(b[i]));
}