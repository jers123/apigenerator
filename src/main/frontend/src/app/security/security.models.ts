import { DependencyDefinition } from "../catalog/catalog.models";

/** Para qué sirve un método de seguridad (espejo de `SecurityRole`). */
export type SecurityRole = 'API_AUTHENTICATION' | 'BROWSER_LOGIN' | 'ADD_ON' | 'AUTHORIZATION_SERVER';

/** Tipos en el orden en que se ofrecen. */
export const SECURITY_ROLES: readonly SecurityRole[] = ['API_AUTHENTICATION', 'BROWSER_LOGIN', 'ADD_ON', 'AUTHORIZATION_SERVER'];

/** Nombre legible de cada tipo, igual que en el backend. */
export const SECURITY_ROLE_LABELS: Readonly<Record<SecurityRole, string>> = {
  API_AUTHENTICATION: 'Autenticación de la API',
  BROWSER_LOGIN: 'Inicio de sesión en navegador',
  ADD_ON: 'Complemento',
  AUTHORIZATION_SERVER: 'Servidor de autorización',
};

/** Tipos cuyos métodos pueden necesitar un emisor de tokens. */
export const ISSUER_ROLES: readonly SecurityRole[] = ['API_AUTHENTICATION', 'BROWSER_LOGIN'];

/**
 * Método de seguridad tal como se guarda (espejo de `SecurityMechanismDefinition`).
 * Es el cuerpo y la respuesta de `/api/v1/security/mechanisms`.
 */
export interface SecurityMechanismDefinition {
  /** Identificador estable; no se puede cambiar después de crearlo. */
  readonly id: string;
  /** Nombre visible. */
  readonly name: string;
  /** Explicación breve. */
  readonly description: string;
  /** Para qué sirve. */
  readonly role: SecurityRole;
  /** Si funciona en proyectos reactivos. */
  readonly reactiveSupported: boolean;
  /** Si necesita un emisor de tokens. */
  readonly requiresIssuer: boolean;
  /** Si el proyecto debe guardar usuarios. */
  readonly managesUsers: boolean;
  /** Ids de los métodos con los que no se puede combinar. */
  readonly conflictsWith: readonly string[];
  /** Artefactos que agrega, en orden. */
  readonly dependencies: readonly DependencyDefinition[];
}

/** Dónde funciona un proveedor de identidad. */
export type ProviderHosting = 'CLOUD' | 'SELF_HOSTED';

/** Alojamientos en el orden en que se ofrecen. */
export const PROVIDER_HOSTINGS: readonly ProviderHosting[] = ['CLOUD', 'SELF_HOSTED'];

/** Nombre legible de cada alojamiento, igual que en el backend. */
export const HOSTING_LABELS: Readonly<Record<ProviderHosting, string>> = {
  CLOUD: 'En la nube',
  SELF_HOSTED: 'Lo instalas tú',
};

/** Dato que pide un proveedor para completar su URL de emisor. */
export interface ProviderParameter {
  /** Nombre del marcador en la plantilla, sin llaves. */
  readonly key: string;
  /** Texto que ve el usuario. */
  readonly label: string;
  /** Valor de ejemplo; vacío si no tiene. */
  readonly example: string;
}

/**
 * Proveedor de identidad tal como se guarda (espejo de `IdentityProviderDefinition`).
 * Es el cuerpo y la respuesta de `/api/v1/security/identity-providers`.
 */
export interface IdentityProviderDefinition {
  /** Identificador estable; no se puede cambiar después de crearlo. */
  readonly id: string;
  /** Nombre visible. */
  readonly name: string;
  /** Explicación breve, con advertencias de configuración. */
  readonly description: string;
  /** Dónde funciona. */
  readonly hosting: ProviderHosting;
  /** URL del emisor con marcadores `{clave}`, o `null` si Spring Boot ya lo conoce. */
  readonly issuerUriTemplate: string | null;
  /** Datos que se piden para completar la plantilla. */
  readonly parameters: readonly ProviderParameter[];
  /** Si la API puede validar sus tokens. */
  readonly supportsResourceServer: boolean;
  /** Si sirve para el inicio de sesión. */
  readonly supportsLogin: boolean;
  /** Documentación oficial, o `null`. */
  readonly documentationUrl: string | null;
}