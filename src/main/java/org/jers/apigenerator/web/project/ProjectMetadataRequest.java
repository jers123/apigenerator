package org.jers.apigenerator.web.project;

import org.jers.apigenerator.core.project.ProjectMetadata;

/**
 * Identidad del proyecto en la petición; las reglas de formato las aplica el núcleo.
 *
 * @param groupId     organización, en minúsculas separadas por puntos
 * @param artifactId  nombre técnico, en minúsculas con guiones
 * @param name        nombre legible
 * @param description descripción opcional
 */
public record ProjectMetadataRequest(String groupId, String artifactId, String name, String description) {

    /**
     * Convierte la petición en metadatos del núcleo.
     *
     * @return metadatos validados
     * @throws org.jers.apigenerator.core.common.DomainValidationException si
     *         algún dato no cumple su formato
     */
    public ProjectMetadata toDomain() {
        return new ProjectMetadata(groupId, artifactId, name, description);
    }
}