package org.jers.apigenerator.core.security;

import java.io.Serial;

/**
 * Se lanza al crear un método de seguridad con un id que ya existe.
 */
public class DuplicateSecurityMechanismException extends RuntimeException {

    /** Versión de serialización de la excepción. */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param id identificador repetido
     */
    public DuplicateSecurityMechanismException(final String id) {
        super("Ya existe un método de seguridad con id '" + id + "'");
    }
}