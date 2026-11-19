package org.jers.apigenerator.infrastructure.database;

import org.jers.apigenerator.core.database.DatabaseEngine;
import org.jers.apigenerator.core.database.DatabaseEngineNotFoundException;
import org.jers.apigenerator.core.database.DatabaseEngineRepository;
import org.jers.apigenerator.core.database.DuplicateDatabaseEngineException;
import org.jers.apigenerator.infrastructure.storage.JsonFileStore;
import org.jers.apigenerator.infrastructure.storage.JsonFileStoreFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Guarda los motores de base de datos en un archivo JSON fuera del JAR, en la
 * carpeta de datos del usuario.
 *
 * <p>La primera vez copia los valores de fábrica. Cada cambio se escribe de
 * forma atómica y solo después se aplica en memoria: si la escritura falla,
 * ni el archivo ni la memoria cambian.</p>
 */
@Component
public class JsonDatabaseEngineRepository implements DatabaseEngineRepository {

    /** Nombre del archivo dentro de la carpeta de datos. */
    static final String FILE_NAME = "databases.json";

    /** Archivo de los motores. */
    private final JsonFileStore<DatabaseEngineFile> store;

    /** Motores en memoria, por id y en orden de inserción. Protegido por {@code this}. */
    private final Map<String, DatabaseEngine> engines = new LinkedHashMap<>();

    /**
     * Prepara el archivo y carga los motores.
     *
     * @param stores   fábrica de archivos de la carpeta de datos
     * @param defaults archivo de fábrica, tomado de {@code app.databases.defaults}
     * @throws IllegalStateException si el archivo no se puede crear, leer o
     *                               contiene motores inválidos o repetidos
     */
    public JsonDatabaseEngineRepository(
            final JsonFileStoreFactory stores,
            @Value("${app.databases.defaults}") final Resource defaults) {
        this.store = stores.create(FILE_NAME, DatabaseEngineFile.class);
        for (DatabaseEngine engine : store.loadOrInitialize(defaults).engines()) {
            if (engines.putIfAbsent(engine.id(), engine) != null) {
                throw new IllegalStateException("Motor repetido en " + store.file() + ": " + engine.id());
            }
        }
    }

    /**
     * Devuelve todos los motores.
     *
     * @return lista inmutable, en orden de inserción
     */
    @Override
    public synchronized List<DatabaseEngine> findAll() {
        return List.copyOf(engines.values());
    }

    /**
     * Busca un motor por su id.
     *
     * @param id identificador del motor
     * @return el motor, o vacío si no existe
     */
    @Override
    public synchronized Optional<DatabaseEngine> findById(final String id) {
        return Optional.ofNullable(engines.get(id));
    }

    /**
     * Agrega un motor y lo guarda en el archivo.
     *
     * @param engine motor validado
     * @return el motor guardado
     * @throws DuplicateDatabaseEngineException si el id ya existe
     */
    @Override
    public synchronized DatabaseEngine create(final DatabaseEngine engine) {
        if (engines.containsKey(engine.id())) {
            throw new DuplicateDatabaseEngineException(engine.id());
        }
        saveWith(engine);
        return engine;
    }

    /**
     * Reemplaza un motor y lo guarda en el archivo.
     *
     * @param engine motor validado
     * @return el motor guardado
     * @throws DatabaseEngineNotFoundException si el id no existe
     */
    @Override
    public synchronized DatabaseEngine update(final DatabaseEngine engine) {
        if (!engines.containsKey(engine.id())) {
            throw new DatabaseEngineNotFoundException(engine.id());
        }
        saveWith(engine);
        return engine;
    }

    /**
     * Escribe el archivo con el motor agregado o reemplazado y, solo si la
     * escritura funciona, actualiza la memoria.
     *
     * @param engine motor a agregar o reemplazar
     */
    private void saveWith(final DatabaseEngine engine) {
        Map<String, DatabaseEngine> next = new LinkedHashMap<>(engines);
        next.put(engine.id(), engine);
        store.write(new DatabaseEngineFile(List.copyOf(next.values())));
        engines.clear();
        engines.putAll(next);
    }

    /**
     * Formato del archivo: un objeto con la lista de motores.
     *
     * @param engines motores guardados
     */
    record DatabaseEngineFile(List<DatabaseEngine> engines) {
    }
}