package org.jers.apigenerator.testsupport;

import org.jers.apigenerator.core.security.SecurityCatalog;
import org.jers.apigenerator.core.security.SecurityCatalogRepository;

import java.util.function.UnaryOperator;

/** Catálogo de seguridad en memoria, con el mismo contrato que el de archivo. */
public final class InMemorySecurityCatalogRepository implements SecurityCatalogRepository {

    private SecurityCatalog catalog;

    public InMemorySecurityCatalogRepository(final SecurityCatalog initial) {
        reset(initial);
    }

    /** Deja el repositorio con el catálogo indicado; para aislar cada test. */
    public synchronized void reset(final SecurityCatalog initial) {
        this.catalog = initial;
    }

    @Override
    public synchronized SecurityCatalog catalog() {
        return catalog;
    }

    @Override
    public synchronized SecurityCatalog modify(final UnaryOperator<SecurityCatalog> change) {
        catalog = change.apply(catalog);
        return catalog;
    }
}