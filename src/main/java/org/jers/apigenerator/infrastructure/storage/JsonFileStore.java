package org.jers.apigenerator.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Archivo JSON editable en la carpeta de datos del usuario.
 *
 * <p>La primera vez se crea copiando un archivo de fábrica. Toda escritura
 * va primero a un archivo temporal en la misma carpeta, que después
 * reemplaza al original de forma atómica: si algo falla, el archivo
 * anterior queda intacto y el temporal se elimina.</p>
 *
 * @param <T> tipo del contenido del archivo
 */
@Slf4j
public final class JsonFileStore<T> {

    /** Sufijo del archivo temporal que se usa durante la escritura. */
    private static final String TEMPORARY_SUFFIX = ".tmp";

    /** Mapeador JSON con salida indentada y booleanos omitidos como {@code false}. */
    private final JsonMapper mapper;

    /** Archivo administrado. */
    private final Path file;

    /** Tipo del contenido, necesario para deserializar. */
    private final Class<T> type;

    /**
     * Crea el almacén. Lo usa {@link JsonFileStoreFactory}, que aporta el
     * mapeador y la carpeta de datos.
     *
     * @param mapper mapeador JSON ya configurado
     * @param file   archivo administrado
     * @param type   tipo del contenido
     */
    JsonFileStore(final JsonMapper mapper, final Path file, final Class<T> type) {
        this.mapper = mapper;
        this.file = file;
        this.type = type;
    }

    /**
     * Devuelve la ruta del archivo administrado.
     *
     * @return ruta absoluta o relativa, tal como se configuró
     */
    public Path file() {
        return file;
    }

    /**
     * Copia el archivo de fábrica si el archivo aún no existe y lo lee.
     *
     * @param defaults archivo de fábrica
     * @return contenido leído y validado por sus constructores
     * @throws IllegalStateException si no se puede copiar, leer o el
     *                               contenido es inválido; el mensaje nombra
     *                               el archivo o el recurso de fábrica
     */
    public T loadOrInitialize(final Resource defaults) {
        try {
            if (Files.notExists(file)) {
                initializeFrom(defaults);
            }
            return mapper.readValue(file.toFile(), type);
        } catch (IOException | JacksonException e) {
            throw new IllegalStateException("No se pudo cargar " + file + ": " + e.getMessage(), e);
        }
    }

    /**
     * Reemplaza el contenido del archivo de forma atómica.
     *
     * @param content contenido nuevo
     * @throws IllegalStateException si la escritura falla; el archivo
     *                               anterior queda intacto
     */
    public void write(final T content) {
        Path temporary = temporaryFile();
        try {
            mapper.writeValue(temporary.toFile(), content);
            replaceWith(temporary);
        } catch (IOException | JacksonException e) {
            discard(temporary, e);
            throw new IllegalStateException("No se pudo guardar " + file + ": " + e.getMessage(), e);
        }
    }

    /**
     * Crea la carpeta y copia el archivo de fábrica pasando por un temporal,
     * para no dejar un archivo a medias si la copia se interrumpe.
     *
     * @param defaults archivo de fábrica
     * @throws IOException si la carpeta o la copia fallan
     */
    private void initializeFrom(final Resource defaults) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temporary = temporaryFile();
        try (InputStream input = defaults.getInputStream()) {
            Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            replaceWith(temporary);
        } catch (IOException e) {
            discard(temporary, e);
            throw e;
        }
        log.info("{} inicializado con los valores de fábrica de {}", file, defaults.getDescription());
    }

    /**
     * Mueve el temporal sobre el archivo definitivo en una sola operación.
     *
     * @param temporary archivo temporal ya escrito
     * @throws IOException si el movimiento falla
     */
    private void replaceWith(final Path temporary) throws IOException {
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Ruta del archivo temporal, en la misma carpeta para que el movimiento
     * sea atómico.
     *
     * @return ruta del temporal
     */
    private Path temporaryFile() {
        return file.resolveSibling(file.getFileName() + TEMPORARY_SUFFIX);
    }

    /**
     * Elimina el temporal tras un fallo. Si tampoco se puede eliminar, lo
     * anota en la causa original sin ocultarla.
     *
     * @param temporary archivo temporal
     * @param cause     error que provocó el descarte
     */
    private static void discard(final Path temporary, final Exception cause) {
        try {
            Files.deleteIfExists(temporary);
        } catch (IOException suppressed) {
            cause.addSuppressed(suppressed);
        }
    }
}