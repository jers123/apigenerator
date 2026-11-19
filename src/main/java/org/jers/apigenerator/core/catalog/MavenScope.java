package org.jers.apigenerator.core.catalog;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Ámbito de una librería dentro de un {@code pom.xml}.
 *
 * <p>{@link #getValue()} devuelve el texto exacto que se escribe en la
 * etiqueta {@code <scope>}.</p>
 */
@Getter
@RequiredArgsConstructor
public enum MavenScope {

    /** Disponible al compilar, probar y ejecutar; es el ámbito por defecto. */
    COMPILE("compile"),

    /** Importa la gestión de versiones de un BOM; solo en dependencyManagement. */
    IMPORT("import"),

    /** Necesaria para compilar; el entorno de ejecución la provee. */
    PROVIDED("provided"),

    /** No se necesita para compilar, solo para ejecutar. */
    RUNTIME("runtime"),

    /** Como {@link #PROVIDED}, pero tomada de una ruta local; en desuso. */
    SYSTEM("system"),

    /** Solo disponible al compilar y ejecutar las pruebas. */
    TEST("test");

    /** Texto que se escribe en la etiqueta {@code <scope>}. */
    private final String value;
}