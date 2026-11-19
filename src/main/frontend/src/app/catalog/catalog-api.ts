import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { Catalog, GroupDefinition, ResolveRequest, ResolveResponse } from './catalog.models';

/**
 * Cliente HTTP de la API del catálogo de dependencias.
 *
 * Las rutas son relativas: en desarrollo las atiende el proxy de
 * `ng serve` y en el JAR las sirve el mismo Spring Boot.
 */
@Service()
export class CatalogApi {
  /** Ruta base de los endpoints del catálogo. */
  private static readonly baseUrl = '/api/v1/catalog';

  /** Cliente HTTP de Angular. */
  private readonly http = inject(HttpClient);

  /**
   * Obtiene el catálogo completo para pintar la pantalla de selección.
   * @returns flujo que emite el catálogo una vez y termina
   */
  getCatalog(): Observable<Catalog> {
    return this.http.get<Catalog>(CatalogApi.baseUrl);
  }

  /**
   * Aplica en el servidor las reglas de selección a lo que marcó el usuario.
   * @param request grupos marcados, herramienta de build y modo reactivo
   * @returns flujo que emite los grupos activos y las dependencias finales;
   *   si la API responde con error, emite un `HttpErrorResponse`
   *   cuyo `error` es un {@link ProblemDetail}
   */
  resolve(request: ResolveRequest): Observable<ResolveResponse> {
    return this.http.post<ResolveResponse>(`${CatalogApi.baseUrl}/resolve`, request);
  }

  /**
   * Obtiene la definición de un grupo para editarla.
   * @param id identificador del grupo
   * @returns flujo que emite el grupo con todas sus dependencias
   */
  getGroup(id: string): Observable<GroupDefinition> {
    return this.http.get<GroupDefinition>(`${CatalogApi.baseUrl}/groups/${encodeURIComponent(id)}`);
  }

  /**
   * Crea un grupo que no es de base de datos.
   * @param group definición del grupo
   * @returns flujo que emite el grupo guardado; si la API lo rechaza, emite
   *   un `HttpErrorResponse` con un {@link ProblemDetail}
   */
  createGroup(group: GroupDefinition): Observable<GroupDefinition> {
    return this.http.post<GroupDefinition>(`${CatalogApi.baseUrl}/groups`, group);
  }

  /**
   * Reemplaza un grupo existente que no es de base de datos; su id indica cuál.
   * @param group nueva definición del grupo
   * @returns flujo que emite el grupo guardado; si la API lo rechaza, emite
   *   un `HttpErrorResponse` con un {@link ProblemDetail}
   */
  updateGroup(group: GroupDefinition): Observable<GroupDefinition> {
    return this.http.put<GroupDefinition>(`${CatalogApi.baseUrl}/groups/${encodeURIComponent(group.id)}`, group);
  }
}