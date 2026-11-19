package org.jers.apigenerator.core.security;

import org.jers.apigenerator.core.common.DomainValidationException;

import java.util.*;

/**
 * Catálogo de métodos de seguridad. Es inmutable y garantiza ids y nombres
 * únicos, choques que apuntan a métodos existentes y como máximo un servidor
 * de autorización.
 *
 * @param mechanisms métodos en el orden en que se muestran; puede estar vacía
 */
public record SecurityCatalog(List<SecurityMechanism> mechanisms) {

    /**
     * Valida el catálogo y protege su lista.
     *
     * @throws NullPointerException      si la lista o alguno de sus elementos es {@code null}
     * @throws DomainValidationException si hay ids o nombres repetidos, un
     *                                   choque con un método inexistente o
     *                                   más de un servidor de autorización
     */
    public SecurityCatalog {
        Objects.requireNonNull(mechanisms, "mechanisms es obligatoria");
        mechanisms = List.copyOf(mechanisms);
        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (SecurityMechanism mechanism : mechanisms) {
            if (!ids.add(mechanism.id())) {
                throw new DomainValidationException("Id de método de seguridad repetido: " + mechanism.id());
            }
            if (!names.add(mechanism.name())) {
                throw new DomainValidationException("Nombre de método de seguridad repetido: " + mechanism.name());
            }
        }
        for (SecurityMechanism mechanism : mechanisms) {
            for (String other : mechanism.conflictsWith()) {
                if (!ids.contains(other)) {
                    throw new DomainValidationException("El método '" + mechanism.id()
                            + "' declara un choque con '" + other + "', que no existe");
                }
            }
        }
        long servers = mechanisms.stream().filter(m -> m.role() == SecurityRole.AUTHORIZATION_SERVER).count();
        if (servers > 1) {
            throw new DomainValidationException("Solo puede haber un método de tipo "
                    + SecurityRole.AUTHORIZATION_SERVER.getLabel() + ", pero hay " + servers);
        }
    }

    /**
     * Busca un método por su id.
     *
     * @param id identificador del método
     * @return el método, o vacío si no existe
     */
    public Optional<SecurityMechanism> findMechanism(final String id) {
        return mechanisms.stream().filter(mechanism -> mechanism.id().equals(id)).findFirst();
    }

    /**
     * Devuelve los métodos de un tipo, en el orden del catálogo.
     *
     * @param role tipo a filtrar
     * @return lista inmutable, posiblemente vacía
     */
    public List<SecurityMechanism> withRole(final SecurityRole role) {
        return mechanisms.stream().filter(mechanism -> mechanism.role() == role).toList();
    }

    /**
     * Indica si dos métodos no se pueden combinar. El choque cuenta en los
     * dos sentidos, aunque solo uno de ellos lo declare.
     *
     * @param first  id de un método
     * @param second id del otro
     * @return {@code true} si alguno declara un choque con el otro
     */
    public boolean conflict(final String first, final String second) {
        return declares(first, second) || declares(second, first);
    }

    /**
     * Devuelve un catálogo nuevo con el método agregado al final.
     *
     * @param mechanism método a agregar
     * @return catálogo nuevo, validado
     * @throws DuplicateSecurityMechanismException si ya existe uno con ese id
     * @throws DomainValidationException           si el nombre ya existe o el
     *                                             catálogo resultante no es válido
     */
    public SecurityCatalog withMechanismAdded(final SecurityMechanism mechanism) {
        Objects.requireNonNull(mechanism, "mechanism es obligatorio");
        if (findMechanism(mechanism.id()).isPresent()) {
            throw new DuplicateSecurityMechanismException(mechanism.id());
        }
        List<SecurityMechanism> next = new ArrayList<>(mechanisms);
        next.add(mechanism);
        return new SecurityCatalog(next);
    }

    /**
     * Devuelve un catálogo nuevo en el que el método con el mismo id se
     * reemplaza, conservando su posición.
     *
     * @param mechanism nueva versión del método
     * @return catálogo nuevo, validado
     * @throws SecurityMechanismNotFoundException si no existe uno con ese id
     * @throws DomainValidationException          si el catálogo resultante no es válido
     */
    public SecurityCatalog withMechanismReplaced(final SecurityMechanism mechanism) {
        Objects.requireNonNull(mechanism, "mechanism es obligatorio");
        if (findMechanism(mechanism.id()).isEmpty()) {
            throw new SecurityMechanismNotFoundException(mechanism.id());
        }
        return new SecurityCatalog(mechanisms.stream()
                .map(current -> current.id().equals(mechanism.id()) ? mechanism : current)
                .toList());
    }

    /**
     * Indica si un método declara un choque con otro.
     *
     * @param id    método que declara
     * @param other método mencionado
     * @return {@code true} si {@code id} existe y menciona a {@code other}
     */
    private boolean declares(final String id, final String other) {
        return findMechanism(id).map(mechanism -> mechanism.conflictsWith().contains(other)).orElse(false);
    }
}