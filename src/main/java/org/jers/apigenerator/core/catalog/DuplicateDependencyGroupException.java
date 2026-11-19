package org.jers.apigenerator.core.catalog;

/**
 * Se lanza al crear un grupo de dependencias con un id que ya existe.
 */
public class DuplicateDependencyGroupException extends RuntimeException {

    /** Versión de serialización de la excepción. */
    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param id identificador repetido
     */
    public DuplicateDependencyGroupException(final String id) {
        super("Ya existe un grupo de dependencias con id '" + id + "'");
    }
}