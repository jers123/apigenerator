package org.jers.apigenerator.application.database;

import lombok.RequiredArgsConstructor;
import org.jers.apigenerator.core.catalog.DependencyCatalogProvider;
import org.jers.apigenerator.core.catalog.DependencyCatalogRepository;
import org.jers.apigenerator.core.catalog.DependencyCategory;
import org.jers.apigenerator.core.catalog.DependencyGroup;
import org.jers.apigenerator.core.common.DomainValidationException;
import org.jers.apigenerator.core.database.DatabaseEngine;
import org.jers.apigenerator.core.database.DatabaseEngineDefinition;
import org.jers.apigenerator.core.database.DatabaseEngineNotFoundException;
import org.jers.apigenerator.core.database.DatabaseEngineRepository;
import org.jers.apigenerator.core.database.DuplicateDatabaseEngineException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Casos de uso de los motores de base de datos: consultar, crear y editar.
 *
 * <p>Cada motor se guarda junto con su grupo de driver. Primero se guarda el
 * grupo en el catálogo y después el motor; si el motor no se puede guardar,
 * se deshace solo el cambio hecho al grupo. Las escrituras se serializan
 * para que dos peticiones no reclamen el mismo grupo a la vez.</p>
 */
@Service
@RequiredArgsConstructor
public class DatabaseEngineService {

    /** Almacenamiento de los motores. */
    private final DatabaseEngineRepository repository;

    /** Almacenamiento del catálogo, donde viven los grupos de driver. */
    private final DependencyCatalogRepository catalogRepository;

    /**
     * Devuelve todos los motores.
     *
     * @return lista inmutable, en orden de creación
     */
    public List<DatabaseEngine> findAll() {
        return repository.findAll();
    }

    /**
     * Busca un motor por su id.
     *
     * @param id identificador del motor
     * @return el motor
     * @throws DatabaseEngineNotFoundException si no existe
     */
    public DatabaseEngine findById(final String id) {
        return repository.findById(id).orElseThrow(() -> new DatabaseEngineNotFoundException(id));
    }

    /**
     * Crea un motor y su grupo de driver. Ninguno de los dos puede existir.
     *
     * @param definition motor y grupo de driver
     * @return el motor guardado
     * @throws DuplicateDatabaseEngineException si ya existe un motor con ese id
     * @throws org.jers.apigenerator.core.catalog.DuplicateDependencyGroupException
     *         si ya existe un grupo con ese id
     * @throws DomainValidationException si el nombre del grupo ya existe
     */
    public synchronized DatabaseEngine create(final DatabaseEngineDefinition definition) {
        DatabaseEngine engine = definition.engine();
        DependencyGroup group = definition.driverGroup();
        if (repository.findById(engine.id()).isPresent()) {
            throw new DuplicateDatabaseEngineException(engine.id());
        }
        catalogRepository.modify(catalog -> catalog.withGroupAdded(group));
        try {
            return repository.create(engine);
        } catch (RuntimeException e) {
            undo(e, () -> catalogRepository.modify(catalog -> catalog.withoutGroup(group.id())));
            throw e;
        }
    }

    /**
     * Edita un motor existente y crea o reemplaza su grupo de driver. El id
     * del motor no se puede cambiar.
     *
     * @param id         id del motor a editar, tomado de la URL
     * @param definition nuevos datos del motor y de su grupo
     * @return el motor guardado
     * @throws DomainValidationException       si el id no coincide, si el
     *                                         grupo es el driver de otro motor,
     *                                         si ya existe y no es de base de
     *                                         datos, o si su nombre ya existe
     * @throws DatabaseEngineNotFoundException si el motor no existe
     */
    public synchronized DatabaseEngine update(final String id, final DatabaseEngineDefinition definition) {
        DatabaseEngine engine = definition.engine();
        DependencyGroup group = definition.driverGroup();
        if (!id.equals(engine.id())) {
            throw new DomainValidationException("El id de la URL ('" + id + "') no coincide con el del cuerpo ('"
                    + engine.id() + "'); el id de un motor no se puede cambiar");
        }
        if (repository.findById(id).isEmpty()) {
            throw new DatabaseEngineNotFoundException(id);
        }
        requireNotUsedByAnotherEngine(engine);

        AtomicReference<DependencyGroup> previous = new AtomicReference<>();
        catalogRepository.modify(catalog -> {
            Optional<DependencyGroup> current = catalog.findGroup(group.id());
            current.ifPresent(DatabaseEngineService::requireDatabaseGroup);
            previous.set(current.orElse(null));
            return current.isPresent() ? catalog.withGroupReplaced(group) : catalog.withGroupAdded(group);
        });
        try {
            return repository.update(engine);
        } catch (RuntimeException e) {
            DependencyGroup old = previous.get();
            undo(e, () -> catalogRepository.modify(catalog ->
                    old == null ? catalog.withoutGroup(group.id()) : catalog.withGroupReplaced(old)));
            throw e;
        }
    }

    /**
     * Verifica que ningún otro motor use ya el grupo de driver.
     *
     * @param engine motor que se está editando
     * @throws DomainValidationException si otro motor usa el mismo grupo
     */
    private void requireNotUsedByAnotherEngine(final DatabaseEngine engine) {
        Optional<DatabaseEngine> owner = repository.findAll().stream()
                .filter(other -> !other.id().equals(engine.id()))
                .filter(other -> other.driverGroupId().equals(engine.driverGroupId()))
                .findFirst();
        if (owner.isPresent()) {
            throw new DomainValidationException("El grupo '" + engine.driverGroupId()
                    + "' ya es el driver del motor '" + owner.get().id() + "'");
        }
    }

    /**
     * Verifica que un grupo existente que se va a reemplazar sea de base de datos.
     *
     * @param existing grupo que ya está en el catálogo
     * @throws DomainValidationException si es de otra categoría
     */
    private static void requireDatabaseGroup(final DependencyGroup existing) {
        if (existing.category() != DependencyCategory.DATABASE) {
            throw new DomainValidationException("El grupo '" + existing.id() + "' ya existe y no es de base de datos ("
                    + existing.category() + "); use otro id para el driver");
        }
    }

    /**
     * Ejecuta una compensación tras un fallo. Si la compensación también
     * falla, se anota en el error original sin ocultarlo.
     *
     * @param cause        error que obligó a compensar
     * @param compensation acción que deshace el cambio
     */
    private static void undo(final RuntimeException cause, final Runnable compensation) {
        try {
            compensation.run();
        } catch (RuntimeException failed) {
            cause.addSuppressed(failed);
        }
    }
}