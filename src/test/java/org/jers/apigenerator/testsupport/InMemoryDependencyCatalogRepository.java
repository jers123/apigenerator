package org.jers.apigenerator.testsupport;

import org.jers.apigenerator.core.catalog.DependencyCatalog;
import org.jers.apigenerator.core.catalog.DependencyCatalogRepository;

import java.util.function.UnaryOperator;

/** Repositorio del catálogo en memoria, con el mismo contrato que el de archivo. */
public final class InMemoryDependencyCatalogRepository implements DependencyCatalogRepository {

    private DependencyCatalog catalog;

    public InMemoryDependencyCatalogRepository(final DependencyCatalog initial) {
        reset(initial);
    }

    /** Deja el repositorio con el catálogo indicado; para aislar cada test. */
    public synchronized void reset(final DependencyCatalog initial) {
        this.catalog = initial;
    }

    @Override
    public synchronized DependencyCatalog catalog() {
        return catalog;
    }

    @Override
    public synchronized void save(final DependencyCatalog next) {
        this.catalog = next;
    }

    @Override
    public synchronized DependencyCatalog modify(final UnaryOperator<DependencyCatalog> change) {
        DependencyCatalog next = change.apply(catalog);
        save(next);
        return next;
    }
}