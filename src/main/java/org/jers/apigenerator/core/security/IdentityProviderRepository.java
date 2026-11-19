package org.jers.apigenerator.core.security;

import java.util.function.UnaryOperator;

/**
 * Puerto de salida para consultar y guardar el catálogo de proveedores de identidad.
 */
public interface IdentityProviderRepository {

    /**
     * Devuelve el catálogo vigente.
     *
     * @return catálogo validado, nunca {@code null}
     */
    IdentityProviderCatalog catalog();

    /**
     * Aplica un cambio al catálogo vigente y guarda el resultado, sin que
     * otro cambio pueda intercalarse entre la lectura y el guardado.
     *
     * @param change función que recibe el catálogo vigente y devuelve el
     *               nuevo; si lanza una excepción, nada cambia
     * @return catálogo guardado
     * @throws IllegalStateException si no se pudo guardar
     */
    IdentityProviderCatalog modify(UnaryOperator<IdentityProviderCatalog> change);
}