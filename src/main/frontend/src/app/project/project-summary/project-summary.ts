import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ConfigFormat, Persistence, ProjectRequest } from '../project.models';
import { ProjectApi } from '../project-api';
import { ID_GENERATIONS, ID_PLACEMENTS, SCALAR_TYPES } from '../../model/model.models';
import { problemMessage } from '../../shared/problem-detail';

/** Texto de cada forma de guardar los datos. */
const PERSISTENCE_LABELS: Readonly<Record<Persistence, string>> = {
  NONE: 'Sin pruebas',
  IN_MEMORY: 'En memoria (RAM)',
  DATABASE: 'Base de datos',
};

/** Archivo de configuración de cada formato. */
const CONFIG_FILES: Readonly<Record<ConfigFormat, string>> = {
  YAML: 'application.yml',
  PROPERTIES: 'application.properties',
};

/** Resumen del modelo para la tarjeta "Modelo". */
interface ModelSummary {
  /** Cantidad de entidades. */
  readonly entities: number;
  /** Cantidad de superclases. */
  readonly superclasses: number;
  /** Cantidad de atributos, propios y de superclases. */
  readonly attributes: number;
  /** Cantidad de relaciones declaradas. */
  readonly relations: number;
  /** Tipo y estrategia del id, por ejemplo `Long · Autoincremento (IDENTITY)`. */
  readonly id: string;
  /** Dónde va el id. */
  readonly placement: string;
  /** Campos de auditoría, o `null` si no tiene. */
  readonly audit: string | null;
}

/**
 * Paso "Resumen": envía la petición al servidor y muestra el plan del
 * proyecto, o el motivo por el que se rechazó.
 */
@Component({
  imports: [MatButtonModule, MatProgressBarModule],
  selector: 'app-project-summary',
  styleUrl: './project-summary.css',
  templateUrl: './project-summary.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectSummary {
  /** Petición armada al llegar al paso, o `null` antes de eso. */
  readonly request = input<ProjectRequest | null>(null);

  /** Cliente de la API del proyecto. */
  private readonly projectApi = inject(ProjectApi);

  /** Texto de cada forma de guardar los datos. */
  protected readonly persistenceLabels = PERSISTENCE_LABELS;

  /** Archivo de configuración de cada formato. */
  protected readonly configFiles = CONFIG_FILES;

  /** Plan calculado por el servidor; se pide de nuevo con cada petición nueva. */
  protected readonly plan = rxResource({
    params: () => this.request() ?? undefined,
    stream: ({ params }) => this.projectApi.plan(params),
  });

  /** Motivo del rechazo del servidor, o `null`. */
  protected readonly errorMessage = computed(() => {
    const error = this.plan.error();
    return error ? problemMessage(error) : null;
  });

  /** Resumen del modelo enviado, o `null` antes de llegar al paso. */
  protected readonly modelSummary = computed<ModelSummary | null>(() => {
    const model = this.request()?.model;
    if (!model) {
      return null;
    }
    const { type, generation, placement } = model.idSettings;
    const audit = [
      model.audit.createdAt ? 'createdAt' : null,
      model.audit.updatedAt ? 'updatedAt' : null,
    ].filter((field): field is string => field !== null);
    return {
      entities: model.entities.length,
      superclasses: model.superclasses.length,
      attributes:
        model.entities.reduce((total, entity) => total + entity.attributes.length, 0)
        + model.superclasses.reduce((total, superclass) => total + superclass.attributes.length, 0),
      relations: model.entities.reduce((total, entity) => total + entity.relations.length, 0),
      id: `${SCALAR_TYPES.find((option) => option.value === type)?.label ?? type} · `
        + `${ID_GENERATIONS.find((option) => option.value === generation)?.label ?? generation}`,
      placement: ID_PLACEMENTS.find((option) => option.value === placement)?.label ?? placement,
      audit: audit.length > 0 ? audit.join(', ') : null,
    };
  });
}