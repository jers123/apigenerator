package org.jers.apigenerator.core.model;

import org.jers.apigenerator.core.common.Preconditions;

import java.util.Objects;

/**
 * Problema encontrado al validar el modelo.
 *
 * @param severity  gravedad
 * @param elementId id interno del elemento afectado (entidad, atributo o
 *                  relación), o {@code null} si es del modelo en general
 * @param location  ubicación legible, por ejemplo {@code Pedido › cliente}
 * @param message   explicación para el usuario
 */
public record ModelIssue(IssueSeverity severity, String elementId, String location, String message) {

    /**
     * Valida que el problema esté completo.
     *
     * @throws NullPointerException si falta la gravedad
     * @throws org.jers.apigenerator.core.common.DomainValidationException si
     *         falta la ubicación o el mensaje
     */
    public ModelIssue {
        Objects.requireNonNull(severity, "severity es obligatoria");
        Preconditions.requireText(location, "location");
        Preconditions.requireText(message, "message");
    }
}