import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { VersionLookupRequest, VersionLookupResponse } from './version.models';

/** Cliente HTTP de la API de versiones de artefactos. */
@Service()
export class VersionApi {

    /** Ruta del endpoint de versiones. */
  private static readonly url = '/api/v1/versions';

  /** Cliente HTTP de Angular. */
  private readonly http = inject(HttpClient);

  /**
   * Consulta las versiones estables de varios artefactos en una sola llamada.
   * @param request artefactos a consultar
   * @returns flujo que emite un resultado por artefacto y termina
   */
  lookup(request: VersionLookupRequest): Observable<VersionLookupResponse> {
    return this.http.post<VersionLookupResponse>(VersionApi.url, request);
  }
}
