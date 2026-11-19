package org.jers.apigenerator.testsupport;

import org.jers.apigenerator.core.model.Attribute;
import org.jers.apigenerator.core.model.AttributeConstraints;
import org.jers.apigenerator.core.model.AttributeType;
import org.jers.apigenerator.core.model.AuditFields;
import org.jers.apigenerator.core.model.Cardinality;
import org.jers.apigenerator.core.model.DomainModel;
import org.jers.apigenerator.core.model.Entity;
import org.jers.apigenerator.core.model.IdGeneration;
import org.jers.apigenerator.core.model.IdPlacement;
import org.jers.apigenerator.core.model.IdSettings;
import org.jers.apigenerator.core.model.Relation;
import org.jers.apigenerator.core.model.ScalarType;
import org.jers.apigenerator.core.model.Superclass;

import java.util.List;

/** Modelos pequeños y predecibles para tests de cualquier capa. */
public final class TestModels {

    private TestModels() {
    }

    /** Atributo simple sin restricciones. */
    public static Attribute attribute(final String id, final String name, final ScalarType type,
                                      final boolean required) {
        return new Attribute(id, name, null, AttributeType.of(type), required, false, AttributeConstraints.none());
    }

    /** Cliente (nombre, email) y Pedido (fecha, total) con Pedido N‑1 Cliente, bidireccional como "pedidos". */
    public static DomainModel clientsAndOrders() {
        Entity client = new Entity("e-cliente", "Cliente", null, null, null,
                List.of(attribute("a-nombre", "nombre", ScalarType.STRING, true),
                        attribute("a-email", "email", ScalarType.STRING, false)),
                List.of());
        Entity order = new Entity("e-pedido", "Pedido", null, null, null,
                List.of(attribute("a-fecha", "fecha", ScalarType.LOCAL_DATE, true),
                        attribute("a-total", "total", ScalarType.BIG_DECIMAL, true)),
                List.of(new Relation("r-cliente", "cliente", "e-cliente", Cardinality.MANY_TO_ONE, true, "pedidos",
                        null, null)));
        return new DomainModel(IdSettings.defaults(), AuditFields.none(), List.of(), List.of(client, order));
    }

    /**
     * Árbol de herencia con el id y la auditoría en la raíz:
     * {@code Auditable ← Documento ← Factura}, {@code Documento ← NotaCredito} y
     * {@code Auditable ← Cliente}.
     */
    public static DomainModel inheritance() {
        Superclass auditable = new Superclass("s-auditable", "Auditable", null,
                List.of(attribute("a-activo", "activo", ScalarType.BOOLEAN, true)));
        Superclass document = new Superclass("s-documento", "Documento", "s-auditable",
                List.of(attribute("a-numero", "numero", ScalarType.STRING, true)));
        Entity invoice = new Entity("e-factura", "Factura", null, null, null, "s-documento",
                List.of(attribute("a-total", "total", ScalarType.BIG_DECIMAL, true)), List.of());
        Entity creditNote = new Entity("e-nota", "NotaCredito", null, null, null, "s-documento",
                List.of(attribute("a-motivo", "motivo", ScalarType.STRING, true)), List.of());
        Entity client = new Entity("e-cliente", "Cliente", null, null, null, "s-auditable",
                List.of(attribute("a-nombre", "nombre", ScalarType.STRING, true)), List.of());
        return new DomainModel(new IdSettings(ScalarType.LONG, IdGeneration.IDENTITY, IdPlacement.ROOT_SUPERCLASS),
                new AuditFields(true, true), List.of(auditable, document), List.of(invoice, creditNote, client));
    }
}