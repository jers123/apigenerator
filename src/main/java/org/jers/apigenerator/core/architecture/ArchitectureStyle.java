package org.jers.apigenerator.core.architecture;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Estilo de arquitectura del proyecto generado.
 *
 * <p>Hexagonal, Onion y Clean comparten columna en la matriz de
 * compatibilidad: protegen el dominio con la misma regla de dependencias y
 * solo cambian los nombres de sus anillos.</p>
 */
@Getter
@RequiredArgsConstructor
public enum ArchitectureStyle {

    /** Controller, service y repository; para CRUD, mantenimientos y prototipos. */
    LAYERED("Capas", "controller → service → repository; para CRUD y mantenimientos", true, 0),

    /** Un paquete por entidad con todo lo suyo dentro, con visibilidad de paquete. */
    PACKAGE_BY_FEATURE("Package-by-feature", "un paquete por entidad con todo lo suyo dentro", false, 1),

    /** Puertos y adaptadores: el dominio no conoce ni la web ni la base de datos. */
    HEXAGONAL("Hexagonal", "dominio y aplicación aislados detrás de puertos; adaptadores de entrada y salida",
            true, 2),

    /** Anillos concéntricos: modelo de dominio, servicios de dominio, aplicación e infraestructura. */
    ONION("Onion", "anillos concéntricos con el modelo de dominio en el centro", true, 2),

    /** Entidades, casos de uso, adaptadores de interfaz y frameworks, según Robert C. Martin. */
    CLEAN("Clean", "entidades, casos de uso, adaptadores de interfaz y frameworks", true, 2),

    /** Una carpeta por caso de uso con su endpoint, su lógica y sus modelos. */
    VERTICAL_SLICE("Vertical Slice", "una carpeta por caso de uso con endpoint, handler y modelos", false, 3),

    /** Módulos de negocio con fronteras verificadas por Spring Modulith y comunicación por eventos. */
    MODULITH("Monolito modular", "módulos de negocio con fronteras verificadas (Spring Modulith) y eventos", true, 4);

    /** Nombre legible. */
    private final String label;

    /** Explicación breve para la interfaz. */
    private final String description;

    /** Si puede generarse como varios módulos de build además de como paquetes. */
    private final boolean multiModule;

    /** Columna del estilo en la matriz de compatibilidad de {@link DesignPattern}. */
    private final int matrixColumn;
}