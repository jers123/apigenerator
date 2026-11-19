package org.jers.apigenerator.infrastructure.version;

import org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Filtra y ordena versiones de Maven.
 *
 * <p>Una versión es estable si no lleva calificadores de prepublicación
 * (SNAPSHOT, alpha, beta, rc, milestone, preview…). El orden lo da
 * {@link ComparableVersion}, el mismo comparador que usa Maven, así que
 * {@code 10.0} va después de {@code 9.9} y {@code 2025.1.0} después de
 * {@code Hoxton.SR12}.</p>
 */
final class StableVersions {

    /** Calificadores de versiones no estables, sin distinguir mayúsculas. */
    private static final Pattern UNSTABLE = Pattern.compile(
            "(?i).*(snapshot|alpha|beta|preview|milestone|incubating|[.-](rc|cr|m|ea|b|dev)\\d*([.-].*)?$).*");

    /** Constructor privado: clase de utilidades sin estado. */
    private StableVersions() {
    }

    /**
     * Devuelve las versiones estables, de la más nueva a la más antigua.
     *
     * @param versions versiones sin filtrar
     * @param limit    cantidad máxima a devolver
     * @return lista inmutable, sin duplicados
     */
    static List<String> newestFirst(final Collection<String> versions, final int limit) {
        return versions.stream()
                .filter(version -> !UNSTABLE.matcher(version).matches())
                .distinct()
                .sorted(Comparator.comparing(ComparableVersion::new).reversed())
                .limit(limit)
                .toList();
    }
}