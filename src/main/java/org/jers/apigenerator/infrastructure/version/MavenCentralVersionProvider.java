package org.jers.apigenerator.infrastructure.version;

import lombok.extern.slf4j.Slf4j;
import org.jers.apigenerator.core.version.ArtifactCoordinates;
import org.jers.apigenerator.core.version.ArtifactVersions;
import org.jers.apigenerator.core.version.VersionProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consulta las versiones en un repositorio Maven leyendo su
 * {@code maven-metadata.xml}, con caché en memoria.
 *
 * <p>Solo se guardan en caché las consultas exitosas: si el repositorio
 * falla, la siguiente petición vuelve a intentarlo.</p>
 */
@Slf4j
@Component
public class MavenCentralVersionProvider implements VersionProvider {

    /** Cantidad máxima de versiones que se ofrecen por artefacto. */
    private static final int MAX_VERSIONS = 50;

    /** Cliente HTTP hacia el repositorio Maven. */
    private final RestClient repository;

    /** Reloj para calcular la caducidad de la caché. */
    private final Clock clock;

    /** Tiempo que una consulta exitosa se mantiene en caché. */
    private final Duration cacheTtl;

    /** Caché de consultas exitosas por artefacto. */
    private final Map<ArtifactCoordinates, CachedVersions> cache = new ConcurrentHashMap<>();

    /**
     * Crea el proveedor.
     *
     * @param mavenRepositoryClient cliente HTTP hacia el repositorio
     * @param clock                 reloj del sistema
     * @param cacheTtl              duración de la caché
     */
    public MavenCentralVersionProvider(
            final RestClient mavenRepositoryClient,
            final Clock clock,
            @Value("${app.versions.cache-ttl}") final Duration cacheTtl) {
        this.repository = mavenRepositoryClient;
        this.clock = clock;
        this.cacheTtl = cacheTtl;
    }

    /**
     * Devuelve las versiones desde la caché si siguen vigentes; si no, las
     * consulta en el repositorio.
     *
     * @param coordinates artefacto a consultar
     * @return versiones estables, o un resultado vacío si la consulta falla
     */
    @Override
    public ArtifactVersions versions(final ArtifactCoordinates coordinates) {
        Instant now = clock.instant();
        CachedVersions cached = cache.get(coordinates);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.versions();
        }
        ArtifactVersions fresh = fetch(coordinates);
        if (fresh.available()) {
            cache.put(coordinates, new CachedVersions(fresh, now.plus(cacheTtl)));
        }
        return fresh;
    }

    /**
     * Descarga y procesa el {@code maven-metadata.xml} del artefacto.
     *
     * @param coordinates artefacto a consultar
     * @return versiones estables, o un resultado vacío si algo falla
     */
    private ArtifactVersions fetch(final ArtifactCoordinates coordinates) {
        String path = coordinates.groupId().replace('.', '/') + "/" + coordinates.artifactId() + "/maven-metadata.xml";
        try {
            byte[] xml = repository.get().uri(path).retrieve().body(byte[].class);
            if (xml == null) {
                return ArtifactVersions.unavailable(coordinates);
            }
            return new ArtifactVersions(coordinates,
                    StableVersions.newestFirst(MavenMetadataParser.versions(xml), MAX_VERSIONS));
        } catch (RestClientException | IllegalArgumentException e) {
            log.warn("No se pudieron consultar las versiones de {}: {}", coordinates, e.getMessage());
            return ArtifactVersions.unavailable(coordinates);
        }
    }

    /**
     * Entrada de la caché.
     *
     * @param versions  resultado de la consulta
     * @param expiresAt instante a partir del cual ya no es válida
     */
    private record CachedVersions(ArtifactVersions versions, Instant expiresAt) {
    }
}