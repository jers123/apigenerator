package org.jers.apigenerator.core.database;

/**
 * Se lanza al crear un motor de base de datos con un id que ya existe.
 */
public class DuplicateDatabaseEngineException extends RuntimeException {

    /** Versión de serialización de la excepción. */
    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param id identificador repetido
     */
    public DuplicateDatabaseEngineException(final String id) {
        super("Ya existe un motor de base de datos con id '" + id + "'");
    }
}