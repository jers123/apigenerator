package org.jers.apigenerator.core.security;

/**
 * Se lanza al buscar o editar un proveedor de identidad que no existe.
 */
public class IdentityProviderNotFoundException extends RuntimeException {

    /** Versión de serialización de la excepción. */
    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param id identificador buscado
     */
    public IdentityProviderNotFoundException(final String id) {
        super("No existe el proveedor de identidad '" + id + "'");
    }
}