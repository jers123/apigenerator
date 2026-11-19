import { HttpErrorResponse } from "@angular/common/http";
import { problemMessage } from "./problem-detail";

describe('problemMessage', () => {
  it('usa el detail del ProblemDetail enviado por la API', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: { title: 'Grupo inexistente', status: 400, detail: 'Grupos inexistentes en el catálogo: [Kafka]' },
    });

    expect(problemMessage(error)).toBe('Grupos inexistentes en el catálogo: [Kafka]');
  });

  it('lo encuentra aunque venga envuelto en cause, como lo entregan los resource', () => {
    const wrapped = new Error('envoltorio', {
      cause: new HttpErrorResponse({ status: 400, error: { detail: 'detalle real' } }),
    });

    expect(problemMessage(wrapped)).toBe('detalle real');
  });

  it('explica que el backend no responde cuando no hay conexión', () => {
    expect(problemMessage(new HttpErrorResponse({ status: 0 }))).toContain('backend');
  });

  it('da un mensaje genérico con el código cuando no hay ProblemDetail', () => {
    expect(problemMessage(new HttpErrorResponse({ status: 503 }))).toContain('503');
  });
});