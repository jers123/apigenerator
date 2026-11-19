import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal } from '@angular/core';
import { rxResource, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormArray, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { debounceTime, map, startWith, switchMap } from 'rxjs';
import { DependencyKind, GradleScope, MavenScope } from '../catalog.models';
import { coordinatesOf, dependencyForm, DependencyForm } from '../group-editor/group-form';
import { ArtifactCoordinates, ArtifactVersions, coordinatesKey, sameArtifacts } from '../../version/version.models';
import { VersionApi } from '../../version/version-api';


/** Pausa tras la última tecla antes de consultar versiones, en milisegundos. */
const LOOKUP_DELAY_MS = 400;

/**
 * Coordenadas completas y sin repetir de las filas.
 * @param rows filas de dependencias
 * @returns artefactos en el orden de las filas
 */
function artifactsOf(rows: FormArray<DependencyForm>): readonly ArtifactCoordinates[] {
  const unique = new Map<string, ArtifactCoordinates>();
  for (const row of rows.controls) {
    const coordinates = coordinatesOf(row);
    if (coordinates) {
      unique.set(coordinatesKey(coordinates), coordinates);
    }
  }
  return [...unique.values()];
}

/**
 * Editor de una lista de dependencias: coordenadas, versión con las
 * publicadas en Maven Central, tipo, ámbitos y marcas.
 *
 * No guarda nada: edita las filas que recibe. Lo usan los grupos del
 * catálogo y los métodos de seguridad.
 */
@Component({
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  selector: 'app-dependency-rows',
  styleUrl: './dependency-rows.css',
  templateUrl: './dependency-rows.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DependencyRows {
  /** Filas a editar; desde aquí nunca queda vacía. */
  readonly rows = input.required<FormArray<DependencyForm>>();

  /** Cliente de la API de versiones. */
  private readonly versionApi = inject(VersionApi);

  /** Tipos de artefacto que se ofrecen. */
  protected readonly kinds: readonly DependencyKind[] = ['LIBRARY', 'PLUGIN', 'BOM', 'PARENT', 'BUILD_TOOL'];

  /** Ámbitos de Maven que se ofrecen. */
  protected readonly mavenScopes: readonly MavenScope[] = ['COMPILE', 'PROVIDED', 'RUNTIME', 'TEST', 'SYSTEM', 'IMPORT'];

  /** Configuraciones de Gradle que se ofrecen. */
  protected readonly gradleScopes: readonly GradleScope[] = [
    'IMPLEMENTATION',
    'API',
    'COMPILE_ONLY',
    'RUNTIME_ONLY',
    'ANNOTATION_PROCESSOR',
    'DEVELOPMENT_ONLY',
    'TEST_IMPLEMENTATION',
    'TEST_COMPILE_ONLY',
    'TEST_RUNTIME_ONLY',
  ];

  /**
   * Artefactos escritos en las filas. Se calcula al recibir las filas y,
   * después, tras una pausa en la escritura.
   */
  private readonly typedArtifacts = toSignal(
    toObservable(this.rows).pipe(
      switchMap((rows) =>
        rows.valueChanges.pipe(
          debounceTime(LOOKUP_DELAY_MS),
          map(() => artifactsOf(rows)),
          startWith(artifactsOf(rows)),
        ),
      ),
    ),
    { initialValue: [] as readonly ArtifactCoordinates[] },
  );

  /** Artefactos a consultar; solo cambia cuando cambia la lista. */
  private readonly versionTargets = computed(() => this.typedArtifacts(), { equal: sameArtifacts });

  /** Versiones publicadas de los artefactos. */
  protected readonly versions = rxResource({
    params: () => (this.versionTargets().length > 0 ? { artifacts: this.versionTargets() } : undefined),
    stream: ({ params }) => this.versionApi.lookup(params),
  });

  /** Versiones por artefacto. Conserva las anteriores mientras llega una consulta nueva. */
  private readonly versionsByKey = linkedSignal<readonly ArtifactVersions[] | undefined, ReadonlyMap<string, ArtifactVersions>>({
    source: () => (this.versions.hasValue() ? this.versions.value().artifacts : undefined),
    computation: (artifacts, previous) =>
      artifacts ? new Map(artifacts.map((artifact) => [coordinatesKey(artifact), artifact])) : (previous?.value ?? new Map()),
  });

  /**
   * Indica si una fila es una librería, que es la única que lleva ámbitos.
   * @param row fila de dependencia
   * @returns `true` si su tipo es `LIBRARY`
   */
  protected isLibrary(row: DependencyForm): boolean {
    return row.controls.kind.value === 'LIBRARY';
  }

  /**
   * Versiones publicadas del artefacto de una fila.
   * @param row fila de dependencia
   * @returns resultado de Maven Central, o `undefined` si faltan
   *   coordenadas o aún no llegó
   */
  protected versionsOf(row: DependencyForm): ArtifactVersions | undefined {
    const coordinates = coordinatesOf(row);
    return coordinates ? this.versionsByKey().get(coordinatesKey(coordinates)) : undefined;
  }

  /**
   * Opciones del desplegable de versión: las publicadas y, si la versión
   * guardada ya no figura entre ellas, también esa, para no perderla.
   * @param row fila de dependencia
   * @param info versiones publicadas del artefacto
   * @returns versiones a ofrecer, sin la opción "Gestionada"
   */
  protected versionOptions(row: DependencyForm, info: ArtifactVersions): readonly string[] {
    const current = row.controls.version.value;
    return current && !info.versions.includes(current) ? [current, ...info.versions] : info.versions;
  }

  /** Agrega una librería en blanco al final. */
  protected addDependency(): void {
    this.rows().push(dependencyForm());
  }

  /**
   * Quita una fila.
   * @param index posición de la fila
   */
  protected removeDependency(index: number): void {
    this.rows().removeAt(index);
  }
}