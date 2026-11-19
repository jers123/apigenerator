package org.jers.apigenerator.infrastructure.security;

import org.jers.apigenerator.core.security.IdentityProviderCatalog;
import org.jers.apigenerator.core.security.IdentityProviderRepository;
import org.jers.apigenerator.infrastructure.storage.JsonFileStore;
import org.jers.apigenerator.infrastructure.storage.JsonFileStoreFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Guarda los proveedores de identidad en un archivo JSON de la carpeta de
 * datos del usuario. Funciona igual que el catálogo de seguridad.
 */
@Component
public class JsonIdentityProviderRepository implements IdentityProviderRepository {

    /** Nombre del archivo dentro de la carpeta de datos. */
    static final String FILE_NAME = "identity-providers.json";

    /** Archivo del catálogo. */
    private final JsonFileStore<IdentityProviderCatalog> store;

    /** Catálogo vigente; se reemplaza entero, nunca se modifica. */
    private volatile IdentityProviderCatalog catalog;

    /**
     * Prepara el archivo y carga el catálogo.
     *
     * @param stores   fábrica de archivos de la carpeta de datos
     * @param defaults catálogo de fábrica, tomado de {@code app.identity-providers.defaults}
     * @throws IllegalStateException si el archivo no se puede crear, leer o es inválido
     */
    public JsonIdentityProviderRepository(
            final JsonFileStoreFactory stores,
            @Value("${app.identity-providers.defaults}") final Resource defaults) {
        this.store = stores.create(FILE_NAME, IdentityProviderCatalog.class);
        this.catalog = store.loadOrInitialize(defaults);
    }

    /**
     * Devuelve el catálogo vigente.
     *
     * @return catálogo validado
     */
    @Override
    public IdentityProviderCatalog catalog() {
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
    public synchronized IdentityProviderCatalog modify(final UnaryOperator<IdentityProviderCatalog> change) {
        IdentityProviderCatalog next = Objects.requireNonNull(change.apply(catalog),
                "el cambio devolvió un catálogo nulo");
        store.write(next);
        this.catalog = next;
        return next;
    }
}