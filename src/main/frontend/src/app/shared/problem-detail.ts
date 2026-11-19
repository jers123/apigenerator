import { HttpErrorResponse } from "@angular/common/http";

/** Error de la API en formato RFC 9457, tal como lo envía `ApiExceptionHandler`. */
export interface ProblemDetail {
    /** Resumen del tipo de problema. */
    readonly title: string;
    /** Código HTTP. */
    readonly status: number;
    /** Explicación concreta de este error. */
    readonly detail: string;
    /** Propiedades adicionales, como `groupIds` en un grupo inexistente. */
    readonly [extra: string]: unknown;
}

/**
 * Convierte cualquier error de una llamada HTTP en un mensaje legible para el usuario.
 *
 * Acepta el {@link HttpErrorResponse} directo o envuelto en `cause`, que es
 * como lo entregan los `resource` de Angular cuando el error no es un `Error`.
 * @param error error recibido de `HttpClient` o de un `resource`
 * @returns el `detail` del {@link ProblemDetail} si la API lo envió;
 *   si no, un mensaje según el tipo de fallo
 */
export function problemMessage(error: unknown): string {
    const response = error instanceof HttpErrorResponse ? error : causeOf(error);
    if (response instanceof HttpErrorResponse) {
        const body = response.error as Partial<ProblemDetail> | null;
        if (typeof body?.detail === 'string') {
            return body.detail;
        }
        if (response.status === 0) {
            return 'No se pudo conectar con el servidor. ¿Está corriendo el backend?';
        }
        return `Error ${response.status} al comunicarse con el servidor`;
    }
    return error instanceof Error ? error.message : 'Error inesperado';
}

/**
 * Extrae la causa de un error envuelto, si la tiene.
 * @param error error recibido
 * @returns la propiedad `cause`, o `undefined`
 */
function causeOf(error: unknown): unknown {
    return typeof error === 'object' && error !== null && 'cause' in error ? error.cause : undefined;
}
