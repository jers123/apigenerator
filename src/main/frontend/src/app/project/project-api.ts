import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { ArchitectureCatalog, ProjectPlan, ProjectRequest } from './project.models';

/** Cliente HTTP de las APIs de arquitecturas y del proyecto a generar. */
@Service()
export class ProjectApi {
    /** Cliente HTTP de Angular. */
  private readonly http = inject(HttpClient);

  /**
   * Obtiene los estilos, repartos y patrones con su compatibilidad.
   * @returns flujo que emite el catálogo una vez y termina
   */
  architectures(): Observable<ArchitectureCatalog> {
    return this.http.get<ArchitectureCatalog>('/api/v1/architectures');
  }

  /**
   * Valida el proyecto y calcula su plan sin generarlo.
   * @param request todo lo que eligió el usuario
   * @returns flujo que emite el plan; si la API lo rechaza, emite un
   *   `HttpErrorResponse` con un {@link ProblemDetail}
   */
  plan(request: ProjectRequest): Observable<ProjectPlan> {
    return this.http.post<ProjectPlan>('/api/v1/projects/plan', request);
  }
}
