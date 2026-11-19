package org.jers.apigenerator.core.version;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Versiones estables publicadas de un artefacto.
 *
 * @param coordinates artefacto consultado
 * @param versions    versiones estables, de la más nueva a la más antigua;
 *                    vacía si no se pudieron consultar
 */
public record ArtifactVersions(ArtifactCoordinates coordinates, List<String> versions) {

    /**
     * Valida el resultado y protege la lista.
     *
     * @throws NullPointerException si las coordenadas, la lista o alguna
     *                              versión es {@code null}
     */
    public ArtifactVersions {
        Objects.requireNonNull(coordinates, "coordinates es obligatorio");
        versions = List.copyOf(versions);
    }

    /**
     * Crea un resultado vacío para un artefacto que no se pudo consultar.
     *
     * @param coordinates artefacto consultado
     * @return resultado sin versiones
     */
    public static ArtifactVersions unavailable(final ArtifactCoordinates coordinates) {
        return new ArtifactVersions(coordinates, List.of());
    }

    /**
     * Indica si se obtuvieron versiones.
     *
     * @return {@code false} si el repositorio no respondió o el artefacto no existe
     */
    public boolean available() {
        return !versions.isEmpty();
    }

    /**
     * Devuelve la versión estable más reciente.
     *
     * @return la primera de la lista, o vacío si no hay versiones
     */
    public Optional<String> latest() {
        return versions.stream().findFirst();
    }
}