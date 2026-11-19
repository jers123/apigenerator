package org.jers.apigenerator.core.catalog;

/**
 * Se lanza al buscar o editar un grupo de dependencias que no existe.
 */
public class DependencyGroupNotFoundException extends RuntimeException {

    /** Versión de serialización de la excepción. */
    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param id identificador buscado
     */
    public DependencyGroupNotFoundException(final String id) {
        super("No existe el grupo de dependencias '" + id + "'");
    }
}