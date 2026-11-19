package org.jers.apigenerator.testsupport;

import org.jers.apigenerator.core.security.IdentityProviderCatalog;
import org.jers.apigenerator.core.security.IdentityProviderRepository;

import java.util.function.UnaryOperator;

/** Proveedores de identidad en memoria, con el mismo contrato que el de archivo. */
public final class InMemoryIdentityProviderRepository implements IdentityProviderRepository {

    private IdentityProviderCatalog catalog;

    public InMemoryIdentityProviderRepository(final IdentityProviderCatalog initial) {
        reset(initial);
    }

    /** Deja el repositorio con el catálogo indicado; para aislar cada test. */
    public synchronized void reset(final IdentityProviderCatalog initial) {
        this.catalog = initial;
    }

    @Override
    public synchronized IdentityProviderCatalog catalog() {
        return catalog;
    }

    @Override
    public synchronized IdentityProviderCatalog modify(final UnaryOperator<IdentityProviderCatalog> change) {
        catalog = change.apply(catalog);
        return catalog;
    }
}