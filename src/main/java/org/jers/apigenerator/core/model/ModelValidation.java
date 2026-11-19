package org.jers.apigenerator.core.model;

import java.util.List;
import java.util.Objects;

/**
 * Resultado de validar un modelo: todos los problemas encontrados.
 *
 * @param issues problemas, en el orden en que se encontraron; vacía si el modelo está bien
 */
public record ModelValidation(List<ModelIssue> issues) {

    /**
     * Protege la lista.
     *
     * @throws NullPointerException si la lista o alguno de sus elementos es {@code null}
     */
    public ModelValidation {
        issues = List.copyOf(Objects.requireNonNull(issues, "issues es obligatoria"));
    }

    /**
     * Indica si hay algún problema que impida generar el proyecto.
     *
     * @return {@code true} si hay al menos un error
     */
    public boolean hasErrors() {
        return issues.stream().anyMatch(issue -> issue.severity() == IssueSeverity.ERROR);
    }

    /**
     * Solo los errores.
     *
     * @return lista inmutable, posiblemente vacía
     */
    public List<ModelIssue> errors() {
        return issues.stream().filter(issue -> issue.severity() == IssueSeverity.ERROR).toList();
    }

    /**
     * Solo los avisos.
     *
     * @return lista inmutable, posiblemente vacía
     */
    public List<ModelIssue> warnings() {
        return issues.stream().filter(issue -> issue.severity() == IssueSeverity.WARNING).toList();
    }
}