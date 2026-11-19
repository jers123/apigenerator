package org.jers.apigenerator.infrastructure.catalog;

import org.jers.apigenerator.core.catalog.DependencyCatalog;
import org.jers.apigenerator.core.catalog.DependencyCatalogRepository;
import org.jers.apigenerator.infrastructure.storage.JsonFileStore;
import org.jers.apigenerator.infrastructure.storage.JsonFileStoreFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Guarda el catálogo de dependencias en un archivo JSON fuera del JAR, en la
 * carpeta de datos del usuario.
 *
 * <p>La primera vez copia el catálogo de fábrica. El archivo se lee y valida
 * al arrancar: si tiene un error, la aplicación no arranca y el mensaje
 * indica el archivo y la causa. Después, el catálogo se sirve desde memoria
 * y cada cambio se escribe de forma atómica antes de publicarse.</p>
 */
@Component
public class JsonDependencyCatalogRepository implements DependencyCatalogRepository {

    /** Nombre del archivo dentro de la carpeta de datos. */
    static final String FILE_NAME = "dependency-catalog.json";

    /** Archivo del catálogo. */
    private final JsonFileStore<DependencyCatalog> store;

    /** Catálogo vigente; se reemplaza entero, nunca se modifica. */
    private volatile DependencyCatalog catalog;

    /**
     * Prepara el archivo y carga el catálogo.
     *
     * @param stores   fábrica de archivos de la carpeta de datos
     * @param defaults catálogo de fábrica, tomado de {@code app.catalog.defaults}
     * @throws IllegalStateException si el archivo no se puede crear, leer o
     *                               viola alguna regla del catálogo
     */
    public JsonDependencyCatalogRepository(
            final JsonFileStoreFactory stores,
            @Value("${app.catalog.defaults}") final Resource defaults) {
        this.store = stores.create(FILE_NAME, DependencyCatalog.class);
        this.catalog = store.loadOrInitialize(defaults);
    }

    /**
     * Devuelve el catálogo vigente.
     *
     * @return catálogo validado
     */
    @Override
    public DependencyCatalog catalog() {
        return catalog;
    }

    /**
     * Escribe el catálogo nuevo y, solo si la escritura funciona, lo publica.
     *
     * @param next catálogo nuevo, ya validado
     * @throws IllegalStateException si no se pudo guardar
     */
    @Override
    public synchronized void save(final DependencyCatalog next) {
        Objects.requireNonNull(next, "catalog es obligatorio");
        store.write(next);
        this.catalog = next;
    }

    /**
     * Calcula el catálogo nuevo a partir del vigente y lo guarda, todo bajo
     * el mismo candado que {@link #save(DependencyCatalog)}.
     *
     * @param change función que recibe el catálogo vigente y devuelve el nuevo
     * @return catálogo guardado
     * @throws IllegalStateException si no se pudo guardar
     */
    @Override
    public synchronized DependencyCatalog modify(final UnaryOperator<DependencyCatalog> change) {
        DependencyCatalog next = change.apply(catalog);
        save(next);
        return next;
    }
}