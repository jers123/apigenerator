package org.jers.apigenerator.core.catalog;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Configuración de Gradle en la que se declara una librería.
 *
 * <p>{@link #getValue()} devuelve el nombre exacto de la configuración que se
 * escribe en el bloque {@code dependencies} de {@code build.gradle}.</p>
 */
@Getter
@RequiredArgsConstructor
public enum GradleScope {

    /** Procesador de anotaciones (Lombok, configuration-processor). */
    ANNOTATION_PROCESSOR("annotationProcessor"),

    /** Expuesta a los consumidores; requiere el plugin {@code java-library}. */
    API("api"),

    /** Solo para compilar; no se empaqueta. */
    COMPILE_ONLY("compileOnly"),

    /** Solo en desarrollo (DevTools); la provee el plugin de Spring Boot. */
    DEVELOPMENT_ONLY("developmentOnly"),

    /** Disponible al compilar y ejecutar; es la configuración habitual. */
    IMPLEMENTATION("implementation"),

    /** Solo para ejecutar (drivers JDBC). */
    RUNTIME_ONLY("runtimeOnly"),

    /** Solo para compilar las pruebas. */
    TEST_COMPILE_ONLY("testCompileOnly"),

    /** Disponible al compilar y ejecutar las pruebas. */
    TEST_IMPLEMENTATION("testImplementation"),

    /** Solo para ejecutar las pruebas (junit-platform-launcher). */
    TEST_RUNTIME_ONLY("testRuntimeOnly");

    /** Nombre de la configuración en {@code build.gradle}. */
    private final String value;
}