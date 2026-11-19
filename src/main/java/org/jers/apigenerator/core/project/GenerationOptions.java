package org.jers.apigenerator.core.project;

import java.util.Objects;

/**
 * Extras que el usuario pide para el código generado.
 *
 * @param javadoc      si cada clase, atributo y método lleva Javadoc en español
 * @param unitTests    si se generan pruebas: unitarias y, si el proyecto tiene
 *                     base de datos, también de integración
 * @param configFormat formato de los archivos de configuración
 */
public record GenerationOptions(boolean javadoc, boolean unitTests, ConfigFormat configFormat) {

    /**
     * Valida las opciones.
     *
     * @throws NullPointerException si falta el formato de configuración
     */
    public GenerationOptions {
        Objects.requireNonNull(configFormat, "configFormat es obligatorio");
    }
}