package org.jers.apigenerator.testsupport;

import org.jers.apigenerator.core.database.DatabaseEngine;
import org.jers.apigenerator.core.database.DatabaseEngineNotFoundException;
import org.jers.apigenerator.core.database.DatabaseEngineRepository;
import org.jers.apigenerator.core.database.DuplicateDatabaseEngineException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Repositorio de motores en memoria, con el mismo contrato que el de archivo. */
public final class InMemoryDatabaseEngineRepository implements DatabaseEngineRepository {

    private final Map<String, DatabaseEngine> engines = new LinkedHashMap<>();

    public InMemoryDatabaseEngineRepository(final DatabaseEngine... initial) {
        reset(initial);
    }

    /** Deja el repositorio solo con los motores indicados; para aislar cada test. */
    public void reset(final DatabaseEngine... initial) {
        engines.clear();
        for (DatabaseEngine engine : initial) {
            engines.put(engine.id(), engine);
        }
    }

    /** Motor PostgreSQL igual al de fábrica. */
    public static DatabaseEngine postgresql() {
        return new DatabaseEngine("postgresql", "PostgreSQL", "postgresql", false, 5432, "postgres",
                "jdbc:postgresql://{host}:{port}/{database}");
    }

    @Override
    public List<DatabaseEngine> findAll() {
        return List.copyOf(engines.values());
    }

    @Override
    public Optional<DatabaseEngine> findById(final String id) {
        return Optional.ofNullable(engines.get(id));
    }

    @Override
    public DatabaseEngine create(final DatabaseEngine engine) {
        if (engines.putIfAbsent(engine.id(), engine) != null) {
            throw new DuplicateDatabaseEngineException(engine.id());
        }
        return engine;
    }

    @Override
    public DatabaseEngine update(final DatabaseEngine engine) {
        if (engines.replace(engine.id(), engine) == null) {
            throw new DatabaseEngineNotFoundException(engine.id());
        }
        return engine;
    }
}