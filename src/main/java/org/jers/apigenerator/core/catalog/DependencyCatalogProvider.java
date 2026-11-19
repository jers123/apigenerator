package org.jers.apigenerator.core.catalog;

/**
 * Puerto de salida que entrega el catálogo de dependencias.
 *
 * <p>El núcleo solo conoce esta interfaz; de dónde sale el catálogo (un JSON,
 * una base de datos, un servicio remoto) lo decide la capa de
 * infraestructura.</p>
 */
@FunctionalInterface
public interface DependencyCatalogProvider {

    /**
     * Devuelve el catálogo vigente.
     *
     * @return catálogo validado, nunca {@code null}
     */
    DependencyCatalog catalog();
}