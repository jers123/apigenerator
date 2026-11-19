package org.jers.apigenerator.core.security;

import org.jers.apigenerator.core.common.Preconditions;

import java.util.regex.Pattern;

/**
 * Dato que el usuario debe escribir para completar la URL del emisor de un
 * proveedor, como el dominio o el id del tenant.
 *
 * @param key     nombre del marcador en la plantilla, sin llaves; por ejemplo {@code tenantId}
 * @param label   texto que ve el usuario
 * @param example valor de ejemplo; vacío si no se indica, nunca {@code null}
 */
public record ProviderParameter(String key, String label, String example) {

    /** Clave de un marcador: letras y números, empezando por letra. */
    static final Pattern KEY = Pattern.compile("[A-Za-z][A-Za-z0-9]*");

    /**
     * Valida el parámetro.
     *
     * @throws org.jers.apigenerator.core.common.DomainValidationException si
     *         la clave no tiene un formato válido o falta la etiqueta
     */
    public ProviderParameter {
        Preconditions.requireFormat(key, KEY, "key", "use letras y números, empezando por letra, por ejemplo tenantId");
        Preconditions.requireText(label, "label");
        example = example == null ? "" : example.strip();
    }
}