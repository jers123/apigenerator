package org.jers.apigenerator.core.project;

import org.jers.apigenerator.core.common.Preconditions;

import java.util.regex.Pattern;

/**
 * Identidad del proyecto generado.
 *
 * @param groupId     organización, en minúsculas separadas por puntos
 * @param artifactId  nombre técnico del artefacto, en minúsculas con guiones
 * @param name        nombre legible del proyecto
 * @param description descripción; vacía si no se indica, nunca {@code null}
 */
public record ProjectMetadata(String groupId, String artifactId, String name, String description) {

    /** Paquete Java en minúsculas: segmentos que empiezan por letra, separados por puntos. */
    private static final Pattern GROUP_ID = Pattern.compile("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*");

    /** Minúsculas y números, con guiones simples entre palabras. */
    private static final Pattern ARTIFACT_ID = Pattern.compile("[a-z][a-z0-9]*(-[a-z0-9]+)*");

    /**
     * Valida los metadatos.
     *
     * @throws org.jers.apigenerator.core.common.DomainValidationException si el groupId o el artifactId no tienen
     *                                  un formato válido o falta el nombre
     */
    public ProjectMetadata {
        Preconditions.requireFormat(groupId, GROUP_ID, "groupId",
                "use minúsculas separadas por puntos, por ejemplo org.jers");
        Preconditions.requireFormat(artifactId, ARTIFACT_ID, "artifactId",
                "use minúsculas, números y guiones, por ejemplo api-clientes");
        Preconditions.requireText(name, "name");
        description = description == null ? "" : description.strip();
    }

    /**
     * Paquete raíz del código generado: el groupId seguido del artifactId sin guiones.
     *
     * @return por ejemplo {@code org.jers.apiclientes} para {@code org.jers} y {@code api-clientes}
     */
    public String basePackage() {
        return groupId + "." + artifactId.replace("-", "");
    }
}