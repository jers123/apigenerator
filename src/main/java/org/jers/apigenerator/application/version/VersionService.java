package org.jers.apigenerator.application.version;

import lombok.RequiredArgsConstructor;
import org.jers.apigenerator.core.version.ArtifactCoordinates;
import org.jers.apigenerator.core.version.ArtifactVersions;
import org.jers.apigenerator.core.version.VersionProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Caso de uso: consultar las versiones de varios artefactos a la vez.
 */
@Service
@RequiredArgsConstructor
public class VersionService {

    /** Fuente de las versiones publicadas. */
    private final VersionProvider versionProvider;

    /**
     * Consulta en paralelo, con un hilo virtual por artefacto.
     *
     * @param artifacts artefactos a consultar; los repetidos se consultan una vez
     * @return un resultado por artefacto distinto, en el orden recibido
     */
    public List<ArtifactVersions> lookup(final List<ArtifactCoordinates> artifacts) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<ArtifactVersions>> futures = artifacts.stream()
                    .distinct()
                    .map(artifact -> CompletableFuture.supplyAsync(() -> versionProvider.versions(artifact), executor))
                    .toList();
            return futures.stream().map(CompletableFuture::join).toList();
        }
    }
}