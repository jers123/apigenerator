package org.jers.apigenerator.web;

import org.jers.apigenerator.core.catalog.DependencyGroupNotFoundException;
import org.jers.apigenerator.core.catalog.DuplicateDependencyGroupException;
import org.jers.apigenerator.core.common.DomainValidationException;
import org.jers.apigenerator.core.database.DatabaseEngineNotFoundException;
import org.jers.apigenerator.core.database.DuplicateDatabaseEngineException;
import org.jers.apigenerator.core.security.DuplicateIdentityProviderException;
import org.jers.apigenerator.core.security.DuplicateSecurityMechanismException;
import org.jers.apigenerator.core.security.IdentityProviderNotFoundException;
import org.jers.apigenerator.core.security.SecurityMechanismNotFoundException;
import org.jers.apigenerator.core.selection.UnknownDependencyGroupException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce las excepciones de la API a respuestas {@link ProblemDetail}
 * (RFC 9457).
 *
 * <p>Al extender {@link ResponseEntityExceptionHandler}, también entrega en
 * ese formato los errores propios de Spring MVC: JSON mal formado,
 * validaciones fallidas, rutas inexistentes, etc.</p>
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /** Título de los errores por datos que violan una regla del dominio. */
    private static final String INVALID_DATA = "Datos inválidos";

    /**
     * Responde 400 cuando se piden grupos que no existen en el catálogo.
     *
     * @param ex excepción con los nombres no encontrados
     * @return detalle del problema con la propiedad {@code groupIds}
     */
    @ExceptionHandler(UnknownDependencyGroupException.class)
    public ProblemDetail handleUnknownGroup(final UnknownDependencyGroupException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Grupo de dependencias inexistente");
        problem.setProperty("groupIds", ex.getUnknownGroups());
        return problem;
    }

    /**
     * Responde 400 cuando los datos enviados violan una regla del dominio.
     *
     * @param ex excepción con la explicación del dato inválido
     * @return detalle del problema
     */
    @ExceptionHandler(DomainValidationException.class)
    public ProblemDetail handleDomainValidation(final DomainValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle(INVALID_DATA);
        return problem;
    }

    /**
     * Responde 400 con el mensaje del dominio cuando un record del núcleo
     * rechaza los datos mientras se lee el JSON; cualquier otro JSON
     * ilegible sigue la respuesta por defecto de Spring.
     *
     * @param ex      error de lectura del cuerpo
     * @param headers cabeceras de la respuesta
     * @param status  estado propuesto por Spring
     * @param request petición en curso
     * @return respuesta con el detalle del problema
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(final HttpMessageNotReadableException ex,
                                                                  final HttpHeaders headers,
                                                                  final HttpStatusCode status,
                                                                  final WebRequest request) {
        DomainValidationException cause = domainCause(ex);
        if (cause == null) {
            return super.handleHttpMessageNotReadable(ex, headers, status, request);
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, cause.getMessage());
        problem.setTitle(INVALID_DATA);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Responde 404 cuando el motor de base de datos pedido no existe.
     *
     * @param ex excepción con el id buscado
     * @return detalle del problema
     */
    @ExceptionHandler(DatabaseEngineNotFoundException.class)
    public ProblemDetail handleEngineNotFound(final DatabaseEngineNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Motor de base de datos inexistente");
        return problem;
    }

    /**
     * Responde 409 cuando se intenta crear un motor con un id que ya existe.
     *
     * @param ex excepción con el id repetido
     * @return detalle del problema
     */
    @ExceptionHandler(DuplicateDatabaseEngineException.class)
    public ProblemDetail handleDuplicateEngine(final DuplicateDatabaseEngineException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Motor de base de datos repetido");
        return problem;
    }

    /**
     * Responde 404 cuando el grupo de dependencias pedido no existe.
     *
     * @param ex excepción con el id buscado
     * @return detalle del problema
     */
    @ExceptionHandler(DependencyGroupNotFoundException.class)
    public ProblemDetail handleGroupNotFound(final DependencyGroupNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Grupo de dependencias no encontrado");
        return problem;
    }

    /**
     * Responde 409 cuando se intenta crear un grupo con un id que ya existe.
     *
     * @param ex excepción con el id repetido
     * @return detalle del problema
     */
    @ExceptionHandler(DuplicateDependencyGroupException.class)
    public ProblemDetail handleDuplicateGroup(final DuplicateDependencyGroupException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Grupo de dependencias repetido");
        return problem;
    }

    /**
     * Responde 404 cuando el método de seguridad pedido no existe.
     *
     * @param ex excepción con el id buscado
     * @return detalle del problema
     */
    @ExceptionHandler(SecurityMechanismNotFoundException.class)
    public ProblemDetail handleMechanismNotFound(final SecurityMechanismNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Método de seguridad no encontrado");
        return problem;
    }

    /**
     * Responde 409 cuando se intenta crear un método de seguridad con un id que ya existe.
     *
     * @param ex excepción con el id repetido
     * @return detalle del problema
     */
    @ExceptionHandler(DuplicateSecurityMechanismException.class)
    public ProblemDetail handleDuplicateMechanism(final DuplicateSecurityMechanismException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Método de seguridad repetido");
        return problem;
    }

    /**
     * Responde 404 cuando el proveedor de identidad pedido no existe.
     *
     * @param ex excepción con el id buscado
     * @return detalle del problema
     */
    @ExceptionHandler(IdentityProviderNotFoundException.class)
    public ProblemDetail handleProviderNotFound(final IdentityProviderNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Proveedor de identidad no encontrado");
        return problem;
    }

    /**
     * Responde 409 cuando se intenta crear un proveedor con un id que ya existe.
     *
     * @param ex excepción con el id repetido
     * @return detalle del problema
     */
    @ExceptionHandler(DuplicateIdentityProviderException.class)
    public ProblemDetail handleDuplicateProvider(final DuplicateIdentityProviderException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Proveedor de identidad repetido");
        return problem;
    }

    /**
     * Busca una regla del dominio violada entre las causas de un error.
     *
     * @param error error recibido
     * @return la excepción del dominio, o {@code null} si no hay
     */
    private static DomainValidationException domainCause(final Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof DomainValidationException domain) {
                return domain;
            }
        }
        return null;
    }
}