package org.jers.apigenerator.core.database;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tipo de base de datos de un motor. Decide qué reglas aplica el modelo:
 * las relaciones con claves foráneas y tablas intermedias solo existen en
 * las relacionales.
 */
@Getter
@RequiredArgsConstructor
public enum DatabaseKind {

    /** Base relacional (SQL): tablas, claves foráneas y JPA. */
    RELATIONAL("Relacional (SQL)", true),

    /** Base de documentos (NoSQL), como MongoDB; reservada para más adelante. */
    DOCUMENT("Documentos (NoSQL)", false);

    /** Nombre legible. */
    private final String label;

    /** Si el generador ya produce proyectos con este tipo de base. */
    private final boolean supported;
}