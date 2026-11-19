import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { DatabaseEngine, DatabaseEngineRequest } from './database.models';

/** Cliente HTTP de la API de motores de base de datos. */
@Service()
export class DatabaseApi {
    /** Ruta base de los endpoints de motores. */
  private static readonly baseUrl = '/api/v1/databases';

  /** Cliente HTTP de Angular. */
  private readonly http = inject(HttpClient);

  /**
   * Lista los motores.
   * @returns flujo que emite los motores en orden de creación
   */
  list(): Observable<readonly DatabaseEngine[]> {
    return this.http.get<readonly DatabaseEngine[]>(DatabaseApi.baseUrl);
  }

  /**
   * Crea un motor junto con su grupo de driver.
   * @param request motor y grupo
   * @returns flujo que emite el motor guardado; si la API lo rechaza, emite
   *   un `HttpErrorResponse` con un {@link ProblemDetail}
   */
  create(request: DatabaseEngineRequest): Observable<DatabaseEngine> {
    return this.http.post<DatabaseEngine>(DatabaseApi.baseUrl, request);
  }

  /**
   * Edita un motor y crea o reemplaza su grupo de driver; su id indica cuál.
   * @param request motor y grupo
   * @returns flujo que emite el motor guardado; si la API lo rechaza, emite
   *   un `HttpErrorResponse` con un {@link ProblemDetail}
   */
  update(request: DatabaseEngineRequest): Observable<DatabaseEngine> {
    return this.http.put<DatabaseEngine>(`${DatabaseApi.baseUrl}/${encodeURIComponent(request.id)}`, request);
  }
}
