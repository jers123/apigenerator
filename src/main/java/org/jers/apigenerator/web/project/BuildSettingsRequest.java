package org.jers.apigenerator.web.project;

import jakarta.validation.constraints.NotNull;
import org.jers.apigenerator.core.project.BuildSettings;
import org.jers.apigenerator.core.project.Packaging;
import org.jers.apigenerator.core.selection.BuildTool;

/**
 * Opciones de construcción en la petición.
 *
 * @param buildTool   herramienta de build
 * @param javaVersion versión de Java
 * @param packaging   formato de empaquetado
 * @param reactive    si usa la pila reactiva
 * @param serverPort  puerto HTTP de la aplicación generada
 */
public record BuildSettingsRequest(
        @NotNull BuildTool buildTool,
        int javaVersion,
        @NotNull Packaging packaging,
        boolean reactive,
        int serverPort) {

    /**
     * Convierte la petición en opciones del núcleo.
     *
     * @return opciones validadas
     * @throws org.jers.apigenerator.core.common.DomainValidationException si
     *         la versión de Java o el puerto no son válidos
     */
    public BuildSettings toDomain() {
        return new BuildSettings(buildTool, javaVersion, packaging, reactive, serverPort);
    }
}