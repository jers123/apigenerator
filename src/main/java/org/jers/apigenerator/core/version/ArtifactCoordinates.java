package org.jers.apigenerator.core.version;

import org.jers.apigenerator.core.common.Preconditions;

import java.util.regex.Pattern;

/**
 * Coordenadas Maven de un artefacto, sin versión.
 *
 * <p>El formato se restringe a los caracteres válidos en Maven. Así las
 * coordenadas pueden convertirse con seguridad en una ruta del repositorio,
 * sin riesgo de que alguien envíe {@code ../} u otra ruta arbitraria.</p>
 *
 * @param groupId    groupId del artefacto
 * @param artifactId artifactId del artefacto
 */
public record ArtifactCoordinates(String groupId, String artifactId) {

    /** Caracteres permitidos en groupId y artifactId de Maven. */
    private static final Pattern MAVEN_ID = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9_.-]*");

    /**
     * Valida las coordenadas.
     *
     * @throws org.jers.apigenerator.core.common.DomainValidationException si alguna parte está vacía o tiene
     *                                  caracteres no permitidos
     */
    public ArtifactCoordinates {
        Preconditions.requireFormat(groupId, MAVEN_ID, "groupId", "solo letras, números, '.', '-' y '_'");
        Preconditions.requireFormat(artifactId, MAVEN_ID, "artifactId", "solo letras, números, '.', '-' y '_'");
    }

    /**
     * Representación estándar {@code groupId:artifactId}.
     *
     * @return coordenadas en formato Maven
     */
    @Override
    public String toString() {
        return groupId + ":" + artifactId;
    }
}