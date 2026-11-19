package org.jers.apigenerator.core.model;

/**
 * Campos de auditoría del modelo; se llenan con Spring Data JPA Auditing.
 * Van donde va el id ({@link IdPlacement}): en cada superclase raíz o en
 * cada entidad.
 *
 * @param createdAt si se agrega la fecha de creación ({@code @CreatedDate})
 * @param updatedAt si se agrega la fecha de última modificación ({@code @LastModifiedDate})
 */
public record AuditFields(boolean createdAt, boolean updatedAt) {

    /** Nombre del campo de fecha de creación. */
    public static final String CREATED_AT = "createdAt";

    /** Nombre del campo de fecha de modificación. */
    public static final String UPDATED_AT = "updatedAt";

    /**
     * Sin auditoría.
     *
     * @return ambos desactivados
     */
    public static AuditFields none() {
        return new AuditFields(false, false);
    }

    /**
     * Indica si hay algún campo de auditoría.
     *
     * @return {@code true} si al menos uno está activo
     */
    public boolean any() {
        return createdAt || updatedAt;
    }
}