package org.jers.apigenerator.core.project;

import org.jers.apigenerator.core.common.DomainValidationException;
import org.jers.apigenerator.core.common.Preconditions;
import org.jers.apigenerator.core.selection.BuildTool;

import java.util.Objects;

/**
 * Opciones de construcción y ejecución del proyecto generado.
 *
 * @param buildTool   herramienta de build
 * @param javaVersion versión de Java del proyecto
 * @param packaging   formato de empaquetado
 * @param reactive    {@code true} si usa la pila reactiva
 * @param serverPort  puerto HTTP de la aplicación generada
 */
public record BuildSettings(
        BuildTool buildTool,
        int javaVersion,
        Packaging packaging,
        boolean reactive,
        int serverPort) {

    /** Versión mínima de Java que exige Spring Boot 4. */
    public static final int MIN_JAVA_VERSION = 17;

    /**
     * Valida las opciones.
     *
     * @throws DomainValidationException si la versión de Java es menor que
     *                                  {@link #MIN_JAVA_VERSION} o el puerto
     *                                  no es válido
     * @throws NullPointerException     si la herramienta o el empaquetado es
     *                                  {@code null}
     */
    public BuildSettings {
        Objects.requireNonNull(buildTool, "buildTool es obligatorio");
        Objects.requireNonNull(packaging, "packaging es obligatorio");
        if (javaVersion < MIN_JAVA_VERSION) {
            throw new DomainValidationException(
                    "javaVersion debe ser " + MIN_JAVA_VERSION + " o superior, pero es " + javaVersion);
        }
        Preconditions.requirePort(serverPort, "serverPort");
    }
}