package org.jers.apigenerator.core.common;

/**
 * Indica que unos datos violan una regla del dominio: un formato, un rango o
 * una combinación inválida.
 *
 * <p>Extiende {@link IllegalArgumentException} porque es un argumento
 * inválido, pero tiene tipo propio para que la capa web la traduzca a un
 * error 400 sin confundirla con fallos de programación, que deben ser 500.</p>
 */
public class DomainValidationException extends IllegalArgumentException {

    /** Versión de serialización de la excepción. */
    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param message explicación del dato inválido, apta para mostrar al usuario
     */
    public DomainValidationException(final String message) {
        super(message);
    }
}