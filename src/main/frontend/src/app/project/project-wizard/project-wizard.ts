import { StepperSelectionEvent } from "@angular/cdk/stepper";
import { ChangeDetectionStrategy, Component, computed, inject, signal } from "@angular/core";
import { rxResource, toSignal } from "@angular/core/rxjs-interop";
import { ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { MatStepperModule } from "@angular/material/stepper";
import { map, startWith } from "rxjs";
import { ArchitectureStep } from "../architecture-step/architecture-step";
import { DatabaseStep } from "../database-step/database-step";
import { DependencyStep } from "../dependency-step/dependency-step";
import { ModelStep } from "../model-step/model-step";
import { ProjectSummary } from "../project-summary/project-summary";
import { basePackageOf, ConnectionForm, enginesWithoutSequences, JAVA_VERSIONS, modelDependencyGroupIds, projectForm, toProjectRequest } from "../project-form";
import { ProjectRequest } from "../project.models";
import { SecurityDependencies, SecurityStep } from "../security-step/security-step";
import { DatabaseApi } from "../../database/database-api";
import { ModelApi } from "../../model/model-api";
import { toDomainModel } from "../../model/model-form";

/** Etiqueta del último paso; al llegar a él se calcula el plan. */
const SUMMARY_LABEL = 'Resumen';

/**
 * Compara dos listas de textos, para no avisar a los pasos si no cambian.
 * @param a lista anterior
 * @param b lista nueva
 * @returns `true` si tienen los mismos textos en el mismo orden
 */
function sameIds(a: readonly string[], b: readonly string[]): boolean {
  return a.length === b.length && a.every((id, i) => id === b[i]);
}

/**
 * Asistente por pasos para crear un proyecto. Cada paso edita su parte del
 * formulario; no se puede avanzar con un paso inválido. Al llegar al resumen,
 * se envía todo al servidor para calcular el plan.
 */
@Component({
  imports: [ReactiveFormsModule, MatButtonModule, MatButtonToggleModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatSlideToggleModule, MatStepperModule, ArchitectureStep, DatabaseStep, SecurityStep, ModelStep, DependencyStep, ProjectSummary],
  selector: 'app-project-wizard',
  styleUrl: './project-wizard.css',
  templateUrl: './project-wizard.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectWizard {
  /** Formulario de todos los pasos. */
  protected readonly form = projectForm();

  /** Versiones de Java que se ofrecen. */
  protected readonly javaVersions = JAVA_VERSIONS;

  /** Etiqueta del paso de resumen, para la plantilla. */
  protected readonly summaryLabel = SUMMARY_LABEL;

  /** Cliente de la API del modelo. */
  private readonly modelApi = inject(ModelApi);

  /** Cliente de la API de motores. */
  private readonly databaseApi = inject(DatabaseApi);

  /** Petición que se muestra en el resumen; se arma al llegar a ese paso. */
  protected readonly planRequest = signal<ProjectRequest | null>(null);

  /** Librerías que informa el paso "Seguridad", para el paso "Dependencias". */
  protected readonly securityDependencies = signal<SecurityDependencies>({ chosen: [], artifacts: [] });

  /** Si el paso "Modelo" permite avanzar. */
  protected readonly modelValid = signal(true);

  /** Grupos que el modelo agrega solos, configurados en el servidor. */
  private readonly modelGroups = rxResource({ stream: () => this.modelApi.dependencyGroups() });

  /** Motores registrados, para saber cuáles tienen secuencias. */
  private readonly engines = rxResource({ stream: () => this.databaseApi.list() });

  /** Se actualiza con cada cambio del formulario. */
  private readonly formChanges = toSignal(this.form.valueChanges.pipe(startWith(null), map(() => Date.now())));

  /** Grupos que el modelo agrega, para bloquearlos en el paso "Dependencias". */
  protected readonly modelGroupIds = computed<readonly string[]>(
    () => {
      this.formChanges();
      const groups = this.modelGroups.hasValue() ? this.modelGroups.value() : null;
      return modelDependencyGroupIds(
        toDomainModel(this.form.controls.model),
        this.form.controls.databases.controls.useDatabase.value,
        groups,
      );
    },
    { equal: sameIds },
  );

  /** Motores elegidos sin secuencias, para el paso "Modelo". */
  protected readonly enginesWithoutSequences = computed<readonly string[]>(
    () => {
      this.formChanges();
      const engines = this.engines.hasValue() ? this.engines.value() : [];
      return enginesWithoutSequences(this.form.controls.databases, engines);
    },
    { equal: sameIds },
  );

  /**
   * Paquete raíz que tendrá el código.
   * @returns el paquete, o `null` si el groupId o el artifactId no son válidos
   */
  protected basePackage(): string | null {
    return basePackageOf(this.form.controls.basics);
  }

  /**
   * Grupo del driver de una base, para el paso de dependencias.
   * @param connection conexión del paso "Bases de datos"
   * @returns el id del grupo, o `null` si esa base no se usa o aún no tiene motor
   */
  protected driverOf(connection: ConnectionForm): string | null {
    return connection.enabled && connection.controls.driverGroupId.value
      ? connection.controls.driverGroupId.value
      : null;
  }

  /**
   * Indica si las pruebas son obligatorias porque hay base de pruebas.
   * @returns `true` si se eligió base de datos de pruebas
   */
  protected testsRequired(): boolean {
    return this.form.controls.databases.controls.useTestDatabase.value;
  }

  /**
   * Estado que se muestra en la opción de pruebas.
   * @returns `true` si el usuario las pidió o si son obligatorias
   */
  protected testsEnabled(): boolean {
    return this.form.controls.options.controls.unitTests.value || this.testsRequired();
  }

  /**
   * Al llegar al resumen, arma la petición con lo elegido en todos los pasos.
   * @param event cambio de paso del stepper
   */
  protected onStepChange(event: StepperSelectionEvent): void {
    if (event.selectedStep.label === SUMMARY_LABEL) {
      this.planRequest.set(toProjectRequest(this.form));
    }
  }
}