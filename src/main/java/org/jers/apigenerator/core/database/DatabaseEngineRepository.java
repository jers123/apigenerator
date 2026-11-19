package org.jers.apigenerator.core.database;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para consultar y guardar motores de base de datos.
 */
public interface DatabaseEngineRepository {

    /**
     * Devuelve todos los motores, en el orden en que se guardaron.
     *
     * @return lista inmutable
     */
    List<DatabaseEngine> findAll();

    /**
     * Busca un motor por su identificador.
     *
     * @param id identificador del motor
     * @return el motor, o vacío si no existe
     */
    Optional<DatabaseEngine> findById(String id);

    /**
     * Agrega un motor nuevo y lo guarda de forma permanente.
     *
     * @param engine motor validado
     * @return el motor guardado
     * @throws DuplicateDatabaseEngineException si ya existe uno con ese id
     */
    DatabaseEngine create(DatabaseEngine engine);

    /**
     * Reemplaza un motor existente y lo guarda de forma permanente.
     *
     * @param engine motor validado; su id identifica cuál se reemplaza
     * @return el motor guardado
     * @throws DatabaseEngineNotFoundException si no existe uno con ese id
     */
    DatabaseEngine update(DatabaseEngine engine);
}