package org.jers.apigenerator.core.security;

import java.io.Serial;

/**
 * Se lanza al buscar o editar un método de seguridad que no existe.
 */
public class SecurityMechanismNotFoundException extends RuntimeException {

    /** Versión de serialización de la excepción. */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param id identificador buscado
     */
    public SecurityMechanismNotFoundException(final String id) {
        super("No existe el método de seguridad '" + id + "'");
    }
}