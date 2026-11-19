package org.jers.apigenerator.core.database;

/**
 * Se lanza al buscar o editar un motor de base de datos que no existe.
 */
public class DatabaseEngineNotFoundException extends RuntimeException {

    /** Versión de serialización de la excepción. */
    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param id identificador buscado
     */
    public DatabaseEngineNotFoundException(final String id) {
        super("No existe el motor de base de datos '" + id + "'");
    }
}