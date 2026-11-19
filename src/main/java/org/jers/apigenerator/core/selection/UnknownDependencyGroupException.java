package org.jers.apigenerator.core.selection;

import java.util.List;

/**
 * Se lanza cuando una petición de selección nombra grupos que no existen en
 * el catálogo.
 *
 * <p>Extiende {@link IllegalArgumentException} porque es un argumento
 * inválido, pero tiene tipo propio para que la capa web pueda traducirla a un
 * error del cliente sin confundirla con fallos de programación.</p>
 */
public class UnknownDependencyGroupException extends IllegalArgumentException {

    /** Versión de serialización de la excepción. */
    private static final long serialVersionUID = 1L;

    /** Nombres de grupo pedidos que no existen, ordenados. */
    private final List<String> unknownGroups;

    /**
     * Crea la excepción con los nombres no encontrados.
     *
     * @param unknownGroups nombres que no existen en el catálogo
     */
    public UnknownDependencyGroupException(final List<String> unknownGroups) {
        super("Grupos inexistentes en el catálogo: " + unknownGroups);
        this.unknownGroups = List.copyOf(unknownGroups);
    }

    /**
     * Devuelve los nombres de grupo que no existen.
     *
     * @return lista inmutable y ordenada
     */
    public List<String> getUnknownGroups() {
        return unknownGroups;
    }
}