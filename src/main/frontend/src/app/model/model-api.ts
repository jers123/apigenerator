import { HttpClient } from "@angular/common/http";
import { inject, Service } from "@angular/core";
import { Observable } from "rxjs";
import { DomainModel, ModelDependencyGroups, ModelValidationResult } from "./model.models";

/** Cliente HTTP de la API del modelo de dominio. */
@Service()
export class ModelApi {
  /** Ruta base del modelo. */
  private static readonly baseUrl = '/api/v1/models';

  /** Cliente HTTP de Angular. */
  private readonly http = inject(HttpClient);

  /**
   * Valida el modelo y calcula sus nombres derivados. Tener errores no es un
   * fallo: llegan en `issues` con `valid` en `false`.
   * @param model modelo tal como está en la pantalla
   * @returns flujo que emite el resultado; si falta un id interno, emite un
   *   `HttpErrorResponse` con un {@link ProblemDetail}
   */
  validate(model: DomainModel): Observable<ModelValidationResult> {
    return this.http.post<ModelValidationResult>(`${ModelApi.baseUrl}/validate`, model);
  }

  /**
   * Obtiene los grupos del catálogo que el modelo agrega solos al proyecto.
   * @returns flujo que emite los ids una vez y termina
   */
  dependencyGroups(): Observable<ModelDependencyGroups> {
    return this.http.get<ModelDependencyGroups>(`${ModelApi.baseUrl}/dependency-groups`);
  }
}