package org.jers.apigenerator.core.version;

/**
 * Puerto de salida que consulta las versiones publicadas de un artefacto.
 */
@FunctionalInterface
public interface VersionProvider {

    /**
     * Consulta las versiones estables de un artefacto.
     *
     * <p>Nunca lanza excepciones por fallos de red: si no puede consultar,
     * devuelve {@link ArtifactVersions#unavailable(ArtifactCoordinates)}.</p>
     *
     * @param coordinates artefacto a consultar
     * @return versiones estables, de la más nueva a la más antigua
     */
    ArtifactVersions versions(ArtifactCoordinates coordinates);
}