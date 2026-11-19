package org.jers.apigenerator.core.project;

/**
 * Dónde guarda los datos el proyecto generado, en ejecución o en pruebas.
 */
public enum Persistence {

    /** No aplica: no se generan pruebas. */
    NONE,

    /** Solo en la RAM del proceso, sin base de datos. */
    IN_MEMORY,

    /** En la base de datos elegida por el usuario. */
    DATABASE
}