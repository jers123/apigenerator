package org.jers.apigenerator.web.project;

import jakarta.validation.constraints.NotNull;
import org.jers.apigenerator.core.project.ConfigFormat;
import org.jers.apigenerator.core.project.GenerationOptions;

/**
 * Opciones de generación en la petición y en la respuesta.
 *
 * @param javadoc      si cada clase, atributo y método lleva Javadoc en español
 * @param unitTests    si se generan pruebas: unitarias y, con base de datos,
 *                     de integración
 * @param configFormat formato de los archivos de configuración
 */
public record GenerationOptionsRequest(boolean javadoc, boolean unitTests, @NotNull ConfigFormat configFormat) {

    /**
     * Convierte la petición en opciones del núcleo.
     *
     * @return opciones de generación
     */
    public GenerationOptions toDomain() {
        return new GenerationOptions(javadoc, unitTests, configFormat);
    }

    /**
     * Convierte las opciones del núcleo para la respuesta.
     *
     * @param options opciones del proyecto
     * @return representación para la API
     */
    public static GenerationOptionsRequest from(final GenerationOptions options) {
        return new GenerationOptionsRequest(options.javadoc(), options.unitTests(), options.configFormat());
    }
}