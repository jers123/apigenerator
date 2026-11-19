import { ChangeDetectionStrategy, Component, computed, effect, inject, input, linkedSignal, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { DependenciesForm } from '../project-form';
import { BuildTool, Dependency, DependencyCategory, DependencyDefinition, DependencyGroup, ResolveRequest } from '../../catalog/catalog.models';
import { CatalogApi } from '../../catalog/catalog-api';
import { problemMessage } from '../../shared/problem-detail';
import { ArtifactCoordinates, ArtifactVersions, coordinatesKey, sameArtifacts } from '../../version/version.models';
import { VersionApi } from '../../version/version-api';

/** Grupos de una misma categoría, tal como se muestran en pantalla. */
interface CategorySection {
  /** Categoría de los grupos. */
  readonly category: DependencyCategory;
  /** Nombre legible de la categoría. */
  readonly label: string;
  /** Grupos de la categoría, en el orden del catálogo. */
  readonly groups: readonly DependencyGroup[];
}

/** Categorías que decide la herramienta de build y no se muestran como casillas. */
const BUILD_TOOL_CATEGORIES: ReadonlySet<DependencyCategory> = new Set<DependencyCategory>(['MAVEN', 'GRADLE']);

/**
 * Compara dos listas de ids, para no repetir cálculos cuando no cambian.
 * @param a lista anterior
 * @param b lista nueva
 * @returns `true` si tienen los mismos ids en el mismo orden
 */
function sameIds(a: readonly string[], b: readonly string[]): boolean {
  return a.length === b.length && a.every((id, i) => id === b[i]);
}

/**
 * Paso "Dependencias" del asistente: grupos, versiones y vista previa en vivo
 * de las dependencias finales que calcula el servidor.
 *
 * Los drivers los decide el paso "Bases de datos", las librerías de
 * seguridad el paso "Seguridad" y los grupos de persistencia y validación el
 * paso "Modelo": aquí se muestran bloqueados y no se guardan en el
 * formulario. Sin base de desarrollo, los grupos que la necesitan quedan
 * bloqueados.
 */
@Component({
  imports: [MatCheckboxModule, MatFormFieldModule, MatProgressBarModule, MatSelectModule],
  selector: 'app-dependency-step',
  styleUrl: './dependency-step.css',
  templateUrl: './dependency-step.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DependencyStep {
  /** Formulario del paso. */
  readonly form = input.required<DependenciesForm>();

  /** Herramienta de build elegida en el paso "Proyecto". */
  readonly buildTool = input.required<BuildTool>();

  /** Modo reactivo elegido en el paso "Proyecto". */
  readonly reactive = input.required<boolean>();

  /** Grupo del driver de la base de desarrollo, o `null` si no hay. */
  readonly databaseDriver = input<string | null>(null);

  /** Grupo del driver de la base de pruebas, o `null` si no hay. */
  readonly testDatabaseDriver = input<string | null>(null);

  /** Artefactos que agregan los métodos elegidos en el paso "Seguridad". */
  readonly securityDependencies = input<readonly DependencyDefinition[]>([]);

  /** Coordenadas `groupId:artifactId` de todas las librerías de seguridad. */
  readonly securityArtifacts = input<readonly string[]>([]);

  /** Grupos que agrega el paso "Modelo". */
  readonly modelGroupIds = input<readonly string[]>([]);

  /** Cliente de la API del catálogo. */
  private readonly catalogApi = inject(CatalogApi);

  /** Cliente de la API de versiones. */
  private readonly versionApi = inject(VersionApi);

  /** Catálogo cargado al abrir el asistente. */
  protected readonly catalog = rxResource({ stream: () => this.catalogApi.getCatalog() });

  /** Grupos del catálogo, o vacía mientras carga o si falló. */
  private readonly catalogGroups = computed<readonly DependencyGroup[]>(() =>
    this.catalog.hasValue() ? this.catalog.value().groups : [],
  );

  /** Si el proyecto tiene base de datos de desarrollo. */
  protected readonly hasDatabase = computed(() => this.databaseDriver() !== null);

  /** Librerías de seguridad, para buscar rápido. */
  private readonly securityArtifactSet = computed(() => new Set(this.securityArtifacts()));

  /** Grupos del modelo, para buscar rápido. */
  private readonly modelGroupSet = computed(() => new Set(this.modelGroupIds()));

  /** Drivers de las bases elegidas, sin repetir y ordenados. */
  private readonly driverGroupIds = computed<readonly string[]>(
    () =>
      [...new Set([this.databaseDriver(), this.testDatabaseDriver()].filter((id): id is string => id !== null))].sort(),
    { equal: sameIds },
  );

  /** Grupos que entran solos: drivers y grupos del modelo, sin repetir y ordenados. */
  private readonly includedGroupIds = computed<readonly string[]>(
    () => [...new Set([...this.driverGroupIds(), ...this.modelGroupIds()])].sort(),
    { equal: sameIds },
  );

  /**
   * Grupos que marcó el usuario. Empieza con los marcados por defecto cuando
   * llega el catálogo. Los bloqueados se conservan aquí, pero no cuentan.
   */
  protected readonly selectedGroups = linkedSignal<ReadonlySet<string>>(
    () =>
      new Set(
        this.catalogGroups()
          .filter((group) => group.selectedByDefault && this.isChoosable(group))
          .map((group) => group.id),
      ),
  );

  /** Grupos marcados que cuentan: sin bloqueados, en el orden del catálogo. */
  private readonly chosenGroupIds = computed<readonly string[]>(
    () =>
      this.catalogGroups()
        .filter((group) => this.selectedGroups().has(group.id) && this.isChoosable(group) && !this.isLocked(group))
        .map((group) => group.id),
    { equal: sameIds },
  );

  /** Secciones visibles, en el orden del catálogo y sin las categorías de herramienta de build. */
  protected readonly sections = computed<readonly CategorySection[]>(() => {
    const byCategory = new Map<DependencyCategory, DependencyGroup[]>();
    for (const group of this.catalogGroups()) {
      if (!BUILD_TOOL_CATEGORIES.has(group.category)) {
        byCategory.set(group.category, [...(byCategory.get(group.category) ?? []), group]);
      }
    }
    return [...byCategory].map(([category, groups]) => ({ category, label: groups[0].categoryLabel, groups }));
  });

  /** Petición al servidor: grupos marcados más los que entran solos; `undefined` sin catálogo. */
  private readonly resolveRequest = computed<ResolveRequest | undefined>(() =>
    this.catalog.hasValue()
      ? {
          groupIds: [...new Set([...this.chosenGroupIds(), ...this.includedGroupIds()])].sort(),
          buildTool: this.buildTool(),
          reactive: this.reactive(),
        }
      : undefined,
  );

  /** Dependencias finales calculadas por el servidor. */
  protected readonly resolution = rxResource({
    params: () => this.resolveRequest(),
    stream: ({ params }) => this.catalogApi.resolve(params),
  });

  /** Últimas dependencias resueltas; conserva las anteriores mientras llegan las nuevas. */
  protected readonly resolvedDependencies = linkedSignal<readonly Dependency[] | undefined, readonly Dependency[]>({
    source: () => (this.resolution.hasValue() ? this.resolution.value().dependencies : undefined),
    computation: (dependencies, previous) => dependencies ?? previous?.value ?? [],
  });

  /** Artefactos cuya versión se puede elegir: todos menos los que siguen al parent. */
  private readonly versionTargets = computed<readonly ArtifactCoordinates[]>(
    () =>
      this.resolvedDependencies()
        .filter((dependency) => !dependency.followsSpringBoot)
        .map(({ groupId, artifactId }) => ({ groupId, artifactId })),
    { equal: sameArtifacts },
  );

  /** Versiones publicadas en Maven Central. */
  protected readonly versions = rxResource({
    params: () => (this.versionTargets().length > 0 ? { artifacts: this.versionTargets() } : undefined),
    stream: ({ params }) => this.versionApi.lookup(params),
  });

  /** Versiones por artefacto; conserva las anteriores mientras llega una consulta. */
  private readonly versionsByKey = linkedSignal<readonly ArtifactVersions[] | undefined, ReadonlyMap<string, ArtifactVersions>>({
    source: () => (this.versions.hasValue() ? this.versions.value().artifacts : undefined),
    computation: (artifacts, previous) =>
      artifacts ? new Map(artifacts.map((artifact) => [coordinatesKey(artifact), artifact])) : (previous?.value ?? new Map()),
  });

  /** Versiones que eligió el usuario, por clave `groupId:artifactId`. */
  private readonly chosenVersions = signal<ReadonlyMap<string, string>>(new Map());

  /** Versión de Spring Boot del proyecto: la del parent. */
  protected readonly springBootVersion = computed(() => {
    const parent = this.resolvedDependencies().find((dependency) => dependency.kind === 'PARENT');
    return parent ? this.versionOf(parent) : null;
  });

  /**
   * Versiones que se envían al servidor: solo las que cambió el usuario,
   * distintas de la del catálogo y de artefactos que siguen en el proyecto.
   */
  private readonly overrides = computed<Readonly<Record<string, string>>>(() => {
    const result: Record<string, string> = {};
    for (const dependency of this.resolvedDependencies()) {
      const chosen = this.chosenVersions().get(coordinatesKey(dependency));
      if (!dependency.followsSpringBoot && chosen && chosen !== dependency.version) {
        result[coordinatesKey(dependency)] = chosen;
      }
    }
    return result;
  });

  /** Mensaje del último error, o `null`. */
  protected readonly errorMessage = computed(() => {
    const error = this.catalog.error() ?? this.resolution.error() ?? this.versions.error();
    return error ? problemMessage(error) : null;
  });

  /** Mantiene el formulario al día con lo que se ve en pantalla. */
  constructor() {
    effect(() => {
      const form = this.form();
      form.controls.groupIds.setValue(this.chosenGroupIds());
      form.controls.versions.setValue(this.overrides());
    });
  }

  /**
   * Indica si un grupo se elige con casilla: ni de build ni de base de datos.
   * @param group grupo del catálogo
   * @returns `true` si el usuario puede marcarlo en algún caso
   */
  private isChoosable(group: DependencyGroup): boolean {
    return !BUILD_TOOL_CATEGORIES.has(group.category) && group.category !== 'DATABASE';
  }

  /**
   * Indica si un grupo lo agrega el paso "Modelo".
   * @param group grupo del catálogo
   * @returns `true` si el modelo lo necesita
   */
  private isModelGroup(group: DependencyGroup): boolean {
    return this.modelGroupSet().has(group.id);
  }

  /**
   * Indica si un grupo trae alguna librería del catálogo de seguridad.
   * @param group grupo del catálogo
   * @returns `true` si alguno de sus artefactos es de seguridad
   */
  private hasSecurityArtifacts(group: DependencyGroup): boolean {
    return [...group.standardDependencies, ...group.reactiveDependencies]
      .some((dependency) => this.securityArtifactSet().has(coordinatesKey(dependency)));
  }

  /**
   * Indica si la casilla de un grupo está bloqueada.
   * @param group grupo del catálogo
   * @returns `true` para los de base de datos, los del modelo, los que traen
   *   librerías de seguridad y, sin base de desarrollo, los que la necesitan
   */
  protected isLocked(group: DependencyGroup): boolean {
    return (
      group.category === 'DATABASE' ||
      this.isModelGroup(group) ||
      this.hasSecurityArtifacts(group) ||
      (group.requiresDatabase && !this.hasDatabase())
    );
  }

  /**
   * Indica si un grupo entra en el proyecto.
   * @param group grupo del catálogo
   * @returns los drivers, si son de una base elegida; los del modelo, siempre;
   *   los demás, si están marcados y no bloqueados
   */
  protected isChecked(group: DependencyGroup): boolean {
    if (group.category === 'DATABASE') {
      return this.driverGroupIds().includes(group.id);
    }
    if (this.isModelGroup(group)) {
      return true;
    }
    return !this.isLocked(group) && this.selectedGroups().has(group.id);
  }

  /**
   * Explicación de por qué un grupo está bloqueado.
   * @param group grupo del catálogo
   * @returns el motivo, o `null` si no está bloqueado
   */
  protected lockReason(group: DependencyGroup): string | null {
    if (group.category === 'DATABASE') {
      return this.driverGroupIds().includes(group.id)
        ? 'Incluido por la base de datos elegida.'
        : 'Se agrega al elegir este motor en «Bases de datos».';
    }
    if (this.isModelGroup(group)) {
      return 'Incluido por el modelo: lo necesitan sus entidades o sus validaciones.';
    }
    if (this.hasSecurityArtifacts(group)) {
      return 'Trae librerías de seguridad: se eligen en «Seguridad».';
    }
    return group.requiresDatabase && !this.hasDatabase() ? 'Necesita una base de datos de desarrollo.' : null;
  }

  /**
   * Marca o desmarca un grupo.
   * @param id id del grupo
   * @param checked nuevo estado de la casilla
   */
  protected toggleGroup(id: string, checked: boolean): void {
    this.selectedGroups.update((current) => {
      const next = new Set(current);
      if (checked) {
        next.add(id);
      } else {
        next.delete(id);
      }
      return next;
    });
  }

  /**
   * Dependencias de un grupo en el modo actual.
   * @param group grupo del catálogo
   * @returns las reactivas en modo reactivo; si no, las estándar
   */
  protected visibleDependencies(group: DependencyGroup): readonly Dependency[] {
    return this.reactive() ? group.reactiveDependencies : group.standardDependencies;
  }

  /**
   * Ámbito de una dependencia según la herramienta de build.
   * @param dependency dependencia del resultado o del paso "Seguridad"
   * @returns el scope de Maven o la configuración de Gradle; si no es librería, su tipo
   */
  protected scopeOf(dependency: Pick<Dependency, 'mavenScope' | 'gradleScope' | 'kind'>): string {
    const scope = this.buildTool() === 'MAVEN' ? dependency.mavenScope : dependency.gradleScope;
    return scope ?? dependency.kind;
  }

  /**
   * Versiones publicadas de una dependencia.
   * @param dependency dependencia del resultado
   * @returns su resultado de Maven Central, o `undefined` si aún no llegó
   */
  protected versionsOf(dependency: Dependency): ArtifactVersions | undefined {
    return this.versionsByKey().get(coordinatesKey(dependency));
  }

  /**
   * Versión vigente: la elegida por el usuario o, si no, la del catálogo.
   * @param dependency dependencia del resultado
   * @returns la versión, o `null` si la gestiona Spring Boot o un BOM
   */
  protected versionOf(dependency: Dependency): string | null {
    return this.chosenVersions().get(coordinatesKey(dependency)) ?? dependency.version;
  }

  /**
   * Opciones del desplegable: las publicadas y, si faltan, la del catálogo y la elegida.
   * @param dependency dependencia del resultado
   * @param info versiones publicadas
   * @returns versiones a ofrecer, sin la opción "Gestionada"
   */
  protected versionOptions(dependency: Dependency, info: ArtifactVersions): readonly string[] {
    const options = [...info.versions];
    for (const extra of [this.versionOf(dependency), dependency.version]) {
      if (extra && !options.includes(extra)) {
        options.unshift(extra);
      }
    }
    return options;
  }

  /**
   * Registra la versión elegida; `null` vuelve a "Gestionada por Spring Boot".
   * @param dependency dependencia del resultado
   * @param version versión elegida, o `null`
   */
  protected chooseVersion(dependency: Dependency, version: string | null): void {
    this.chosenVersions.update((current) => {
      const next = new Map(current);
      if (version === null) {
        next.delete(coordinatesKey(dependency));
      } else {
        next.set(coordinatesKey(dependency), version);
      }
      return next;
    });
  }
}