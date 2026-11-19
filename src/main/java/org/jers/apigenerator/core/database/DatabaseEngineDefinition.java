package org.jers.apigenerator.core.database;

import org.jers.apigenerator.core.catalog.DependencyCategory;
import org.jers.apigenerator.core.catalog.DependencyGroup;
import org.jers.apigenerator.core.common.DomainValidationException;

import java.util.Objects;

/**
 * Motor de base de datos junto con el grupo del catálogo que aporta su
 * driver. Se crean y editan juntos para que ningún motor quede sin driver.
 *
 * @param engine      motor validado
 * @param driverGroup grupo de base de datos cuyo id es el
 *                    {@code driverGroupId} del motor
 */
public record DatabaseEngineDefinition(DatabaseEngine engine, DependencyGroup driverGroup) {

    /**
     * Verifica que el grupo sea de base de datos y que corresponda al motor.
     *
     * @throws NullPointerException      si falta el motor o el grupo
     * @throws DomainValidationException si el grupo no es de la categoría
     *                                   {@link DependencyCategory#DATABASE} o
     *                                   su id no coincide con el
     *                                   {@code driverGroupId} del motor
     */
    public DatabaseEngineDefinition {
        Objects.requireNonNull(engine, "engine es obligatorio");
        Objects.requireNonNull(driverGroup, "driverGroup es obligatorio");
        if (driverGroup.category() != DependencyCategory.DATABASE) {
            throw new DomainValidationException("El grupo de driver '" + driverGroup.id()
                    + "' debe ser de categoría DATABASE, pero es " + driverGroup.category());
        }
        if (!engine.driverGroupId().equals(driverGroup.id())) {
            throw new DomainValidationException("El driverGroupId del motor ('" + engine.driverGroupId()
                    + "') no coincide con el id de su grupo ('" + driverGroup.id() + "')");
        }
    }
}