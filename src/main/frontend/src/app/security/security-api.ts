import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { IdentityProviderDefinition, SecurityMechanismDefinition } from './security.models';

/** Cliente HTTP de la API de seguridad: métodos y proveedores de identidad. */
@Service()
export class SecurityApi {
  /** Ruta base de los métodos de seguridad. */
  private static readonly mechanismsUrl = '/api/v1/security/mechanisms';

  /** Ruta base de los proveedores de identidad. */
  private static readonly providersUrl = '/api/v1/security/identity-providers';

  /** Cliente HTTP de Angular. */
  private readonly http = inject(HttpClient);

  /**
   * Lista los métodos de seguridad.
   * @returns flujo que emite los métodos en el orden del catálogo
   */
  mechanisms(): Observable<readonly SecurityMechanismDefinition[]> {
    return this.http.get<readonly SecurityMechanismDefinition[]>(SecurityApi.mechanismsUrl);
  }

  /**
   * Crea un método de seguridad.
   * @param mechanism método a crear
   * @returns flujo que emite el método guardado; si la API lo rechaza, emite
   *   un `HttpErrorResponse` con un {@link ProblemDetail}
   */
  createMechanism(mechanism: SecurityMechanismDefinition): Observable<SecurityMechanismDefinition> {
    return this.http.post<SecurityMechanismDefinition>(SecurityApi.mechanismsUrl, mechanism);
  }

  /**
   * Edita un método de seguridad; su id indica cuál.
   * @param mechanism nueva versión del método
   * @returns flujo que emite el método guardado; si la API lo rechaza, emite
   *   un `HttpErrorResponse` con un {@link ProblemDetail}
   */
  updateMechanism(mechanism: SecurityMechanismDefinition): Observable<SecurityMechanismDefinition> {
    return this.http.put<SecurityMechanismDefinition>(
      `${SecurityApi.mechanismsUrl}/${encodeURIComponent(mechanism.id)}`, mechanism);
  }

  /**
   * Lista los proveedores de identidad.
   * @returns flujo que emite los proveedores en el orden del catálogo
   */
  providers(): Observable<readonly IdentityProviderDefinition[]> {
    return this.http.get<readonly IdentityProviderDefinition[]>(SecurityApi.providersUrl);
  }

  /**
   * Crea un proveedor de identidad.
   * @param provider proveedor a crear
   * @returns flujo que emite el proveedor guardado; si la API lo rechaza,
   *   emite un `HttpErrorResponse` con un {@link ProblemDetail}
   */
  createProvider(provider: IdentityProviderDefinition): Observable<IdentityProviderDefinition> {
    return this.http.post<IdentityProviderDefinition>(SecurityApi.providersUrl, provider);
  }

  /**
   * Edita un proveedor de identidad; su id indica cuál.
   * @param provider nueva versión del proveedor
   * @returns flujo que emite el proveedor guardado; si la API lo rechaza,
   *   emite un `HttpErrorResponse` con un {@link ProblemDetail}
   */
  updateProvider(provider: IdentityProviderDefinition): Observable<IdentityProviderDefinition> {
    return this.http.put<IdentityProviderDefinition>(
      `${SecurityApi.providersUrl}/${encodeURIComponent(provider.id)}`, provider);
  }
}