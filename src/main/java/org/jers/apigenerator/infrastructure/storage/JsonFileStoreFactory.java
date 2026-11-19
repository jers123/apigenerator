package org.jers.apigenerator.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;

/**
 * Crea los {@link JsonFileStore} de la carpeta de datos del usuario con una
 * configuración común.
 *
 * <p>Todos los archivos comparten el mismo mapeador: salida indentada, para
 * que se puedan editar a mano, y booleanos omitidos leídos como
 * {@code false}. El mapeador de la API no se modifica.</p>
 */
@Component
public class JsonFileStoreFactory {

    /** Mapeador común de los archivos de datos. */
    private final JsonMapper mapper;

    /** Carpeta de datos del usuario. */
    private final Path dataDir;

    /**
     * Prepara el mapeador común.
     *
     * @param jsonMapper mapeador JSON configurado por Spring Boot
     * @param dataDir    carpeta de datos, tomada de {@code app.data-dir}
     */
    public JsonFileStoreFactory(
            final JsonMapper jsonMapper,
            @Value("${app.data-dir}") final Path dataDir) {
        this.mapper = jsonMapper.rebuild()
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
        this.dataDir = dataDir;
    }

    /**
     * Crea el almacén de un archivo de la carpeta de datos.
     *
     * @param fileName nombre del archivo dentro de la carpeta
     * @param type     tipo del contenido
     * @param <T>      tipo del contenido
     * @return almacén del archivo; no lo crea ni lo lee todavía
     */
    public <T> JsonFileStore<T> create(final String fileName, final Class<T> type) {
        return new JsonFileStore<>(mapper, dataDir.resolve(fileName), type);
    }
}