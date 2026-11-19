package org.jers.apigenerator.infrastructure.security;

import org.jers.apigenerator.core.security.SecurityCatalog;
import org.jers.apigenerator.core.security.SecurityCatalogRepository;
import org.jers.apigenerator.infrastructure.storage.JsonFileStore;
import org.jers.apigenerator.infrastructure.storage.JsonFileStoreFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Guarda los métodos de seguridad en un archivo JSON de la carpeta de datos
 * del usuario.
 *
 * <p>La primera vez copia los valores de fábrica. El archivo se lee y valida
 * al arrancar; después, el catálogo se sirve desde memoria y cada cambio se
 * escribe de forma atómica antes de publicarse.</p>
 */
@Component
public class JsonSecurityCatalogRepository implements SecurityCatalogRepository {

    /** Nombre del archivo dentro de la carpeta de datos. */
    static final String FILE_NAME = "security.json";

    /** Archivo del catálogo. */
    private final JsonFileStore<SecurityCatalog> store;

    /** Catálogo vigente; se reemplaza entero, nunca se modifica. */
    private volatile SecurityCatalog catalog;

    /**
     * Prepara el archivo y carga el catálogo.
     *
     * @param stores   fábrica de archivos de la carpeta de datos
     * @param defaults catálogo de fábrica, tomado de {@code app.security.defaults}
     * @throws IllegalStateException si el archivo no se puede crear, leer o es inválido
     */
    public JsonSecurityCatalogRepository(
            final JsonFileStoreFactory stores,
            @Value("${app.security.defaults}") final Resource defaults) {
        this.store = stores.create(FILE_NAME, SecurityCatalog.class);
        this.catalog = store.loadOrInitialize(defaults);
    }

    /**
     * Devuelve el catálogo vigente.
     *
     * @return catálogo validado
     */
    @Override
    public SecurityCatalog catalog() {
        return catalog;
    }

    /**
     * Calcula el catálogo nuevo a partir del vigente, lo escribe y, solo si la
     * escritura funciona, lo publica.
     *
     * @param change función que recibe el catálogo vigente y devuelve el nuevo
     * @return catálogo guardado
     * @throws IllegalStateException si no se pudo guardar
     */
    @Override
    public synchronized SecurityCatalog modify(final UnaryOperator<SecurityCatalog> change) {
        SecurityCatalog next = Objects.requireNonNull(change.apply(catalog), "el cambio devolvió un catálogo nulo");
        store.write(next);
        this.catalog = next;
        return next;
    }
}