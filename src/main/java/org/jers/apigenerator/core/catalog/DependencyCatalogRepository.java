package org.jers.apigenerator.core.catalog;

import java.util.function.UnaryOperator;

/**
 * Puerto de salida que, además de entregar el catálogo vigente, permite
 * reemplazarlo de forma permanente.
 *
 * <p>Se separa de {@link DependencyCatalogProvider} para que quien solo lee
 * el catálogo no dependa de la capacidad de modificarlo.</p>
 */
public interface DependencyCatalogRepository extends DependencyCatalogProvider {

    /**
     * Reemplaza el catálogo completo y lo guarda de forma permanente.
     *
     * <p>Si el guardado falla, el catálogo vigente no cambia.</p>
     *
     * @param catalog catálogo nuevo, ya validado
     * @throws IllegalStateException si no se pudo guardar
     */
    void save(DependencyCatalog catalog);

    /**
     * Aplica un cambio al catálogo vigente y guarda el resultado, sin que otro
     * cambio pueda intercalarse entre la lectura y el guardado.
     *
     * @param change función que recibe el catálogo vigente y devuelve el
     *               nuevo; si lanza una excepción, nada cambia
     * @return catálogo guardado
     * @throws IllegalStateException si no se pudo guardar
     */
    DependencyCatalog modify(UnaryOperator<DependencyCatalog> change);
}