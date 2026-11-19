package org.jers.apigenerator.core.security;

/**
 * Se lanza al crear un proveedor de identidad con un id que ya existe.
 */
public class DuplicateIdentityProviderException extends RuntimeException {

    /** Versión de serialización de la excepción. */
    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param id identificador repetido
     */
    public DuplicateIdentityProviderException(final String id) {
        super("Ya existe un proveedor de identidad con id '" + id + "'");
    }
}