package org.jers.apigenerator.core.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Dónde se declaran el id y la auditoría. Es la misma para todo el modelo.
 */
@Getter
@RequiredArgsConstructor
public enum IdPlacement {

	/** En cada entidad; las superclases, si hay, solo aportan atributos comunes. */
	ENTITY("En cada entidad"),

	/** En cada superclase raíz; toda entidad debe heredar de una superclase. */
	ROOT_SUPERCLASS("En la superclase raíz");

	/** Nombre legible. */
	private final String label;
}