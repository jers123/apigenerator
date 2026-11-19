package org.jers.apigenerator.application.catalog;

import lombok.RequiredArgsConstructor;
import org.jers.apigenerator.core.catalog.DependencyCatalog;
import org.jers.apigenerator.core.catalog.DependencyCatalogProvider;
import org.jers.apigenerator.core.selection.DependencySelector;
import org.jers.apigenerator.core.selection.ResolvedSelection;
import org.jers.apigenerator.core.selection.SelectionRequest;
import org.springframework.stereotype.Service;

/**
 * Casos de uso del catálogo de dependencias: consultarlo y resolver una
 * selección.
 *
 * <p>Coordina el puerto del catálogo con las reglas del núcleo; no contiene
 * reglas propias.</p>
 */
@Service
@RequiredArgsConstructor
public class CatalogService {

    /**
     * Fuente del catálogo vigente.
     */
    private final DependencyCatalogProvider catalogProvider;

    /**
     * Devuelve el catálogo completo.
     *
     * @return catálogo vigente
     */
    public DependencyCatalog catalog() {
        return catalogProvider.catalog();
    }

    /**
     * Aplica las reglas de selección a la elección del usuario.
     *
     * @param request elección del usuario
     * @return grupos activos y dependencias finales
     * @throws org.jers.apigenerator.core.selection.UnknownDependencyGroupException si se pide un grupo inexistente
     */
    public ResolvedSelection resolve(final SelectionRequest request) {
        return new DependencySelector(catalogProvider.catalog()).select(request);
    }
}