package org.jers.apigenerator.core.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Gravedad de un problema del modelo.
 */
@Getter
@RequiredArgsConstructor
public enum IssueSeverity {

    /** Impide generar el proyecto. */
    ERROR("Error"),

    /** Se puede generar, pero conviene revisarlo. */
    WARNING("Aviso");

    /** Nombre legible. */
    private final String label;
}