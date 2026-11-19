package org.jers.apigenerator.core.architecture;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

/**
 * Patrón de diseño que puede formar parte del proyecto generado, con su
 * compatibilidad con cada estilo de arquitectura.
 *
 * <p>La matriz de cada patrón tiene un símbolo por columna, en el orden
 * Capas, Package-by-feature, Hexagonal/Onion/Clean, Vertical Slice y
 * Modulith: {@code N} nativo, {@code C} compatible, {@code X}
 * contraindicado, {@code -} no aplica.</p>
 */
public enum DesignPattern {

    // ── Empresariales y de dominio ──────────────────────────────────

    /** Un procedimiento por operación de negocio. */
    TRANSACTION_SCRIPT("Transaction Script", PatternFamily.ENTERPRISE, true, "NNCNC", null),

    /** Entidades con comportamiento y reglas propias. */
    DOMAIN_MODEL("Domain Model (rico)", PatternFamily.ENTERPRISE, true, "CCNCN", null),

    /** Capa que define las operaciones disponibles y coordina la respuesta. */
    SERVICE_LAYER("Service Layer", PatternFamily.ENTERPRISE, true, "NNNXN",
            "en Vertical Slice una capa de servicios compartida reintroduce el acoplamiento horizontal "
                    + "que el estilo elimina"),

    /** Colección de agregados detrás de una interfaz del dominio. */
    REPOSITORY("Repository (DDD)", PatternFamily.ENTERPRISE, true, "CCNCN", null),

    /** Interfaces de Spring Data inyectadas directamente en la lógica. */
    SPRING_DATA_REPOSITORY("Spring Data directo", PatternFamily.ENTERPRISE, true, "NNXNC",
            "en Hexagonal, Onion y Clean un JpaRepository en el caso de uso invierte la dirección de las "
                    + "dependencias e impide probarlo sin Spring"),

    /** Traduce entre objetos y tablas (JPA). */
    DATA_MAPPER("Data Mapper (JPA)", PatternFamily.ENTERPRISE, true, "NNNNN", null),

    /** Objetos de transferencia en la frontera de la API, con su mapper. */
    DTO("DTO + Mapper", PatternFamily.ENTERPRISE, true, "NNNNN", null),

    /** La interfaz vive en el dominio y su implementación fuera (puertos). */
    SEPARATED_INTERFACE("Separated Interface", PatternFamily.ENTERPRISE, true, "CCNXN",
            "en Vertical Slice cada slice accede a sus datos directamente; interfaces compartidas "
                    + "reintroducen fronteras horizontales"),

    /** Reglas de negocio combinables como objetos. */
    SPECIFICATION("Specification", PatternFamily.ENTERPRISE, false, "CCCCC", null),

    /** Algo relevante que ocurrió en el dominio, publicado para otros módulos. */
    DOMAIN_EVENT("Domain Event", PatternFamily.ENTERPRISE, true, "XCNCN",
            "en capas no hay fronteras internas que cruzar: el evento oculta una llamada que sigue siendo "
                    + "síncrona en la misma transacción"),

    // ── Arquitectónicos ─────────────────────────────────────────────

    /** Un objeto por petición de negocio, atendido por su caso de uso. */
    COMMAND_USE_CASE("Command / Use Case", PatternFamily.ARCHITECTURAL, true, "CCNNN", null),

    /** Modelos separados para escribir y para leer. */
    CQRS("CQRS", PatternFamily.ARCHITECTURAL, true, "XCCNC",
            "en capas ya hay un único modelo que atraviesa todas las capas; CQRS duplicaría las capas sin "
                    + "ganancia"),

    /** El estado se reconstruye a partir de la secuencia de eventos. */
    EVENT_SOURCING("Event Sourcing", PatternFamily.ARCHITECTURAL, false, "XXCCC",
            "en Capas y Package-by-feature no hay un modelo de dominio que reconstruir a partir de eventos"),

    /** Núcleo mínimo extensible con módulos que se conectan como plug-ins. */
    MICROKERNEL("Microkernel / Plugin", PatternFamily.ARCHITECTURAL, false, "XCCCN",
            "en capas no hay módulos independientes que puedan funcionar como plug-ins"),

    // ── GoF creacionales ────────────────────────────────────────────

    /** Familias de objetos relacionados sin nombrar sus clases concretas. */
    ABSTRACT_FACTORY("Abstract Factory", PatternFamily.CREATIONAL, false, "CCCCC", null),

    /** Construcción paso a paso de objetos complejos. */
    BUILDER("Builder", PatternFamily.CREATIONAL, false, "CCCCC", null),

    /** Las subclases deciden qué objeto crear. */
    FACTORY_METHOD("Factory Method", PatternFamily.CREATIONAL, false, "CCCCC", null),

    /** Nuevos objetos copiando uno existente. */
    PROTOTYPE("Prototype", PatternFamily.CREATIONAL, false, "CCCCC", null),

    /** Una única instancia; en Spring, el ámbito por defecto de los beans. */
    SINGLETON("Singleton", PatternFamily.CREATIONAL, false, "CCCCC", null),

    // ── GoF estructurales ───────────────────────────────────────────

    /** Traduce una interfaz a otra; en hexagonal, cada adaptador. */
    ADAPTER("Adapter", PatternFamily.STRUCTURAL, true, "CCNCN", null),

    /** Separa una abstracción de su implementación. */
    BRIDGE("Bridge", PatternFamily.STRUCTURAL, false, "CCCCC", null),

    /** Trata igual a un objeto y a un grupo de objetos. */
    COMPOSITE("Composite", PatternFamily.STRUCTURAL, false, "CCCCC", null),

    /** Añade comportamiento envolviendo un objeto. */
    DECORATOR("Decorator", PatternFamily.STRUCTURAL, false, "CCCCC", null),

    /** Punto de entrada simple a un subsistema. */
    FACADE("Facade", PatternFamily.STRUCTURAL, true, "CCNXN",
            "en Vertical Slice una fachada compartida entre slices reintroduce el acoplamiento horizontal"),

    /** Comparte el estado común de muchos objetos pequeños. */
    FLYWEIGHT("Flyweight", PatternFamily.STRUCTURAL, false, "CCCCC", null),

    /** Sustituto que controla el acceso a otro objeto; en Spring, la base de AOP. */
    PROXY("Proxy", PatternFamily.STRUCTURAL, false, "CCCCC", null),

    // ── GoF de comportamiento ───────────────────────────────────────

    /** Pasa una petición por una cadena de manejadores. */
    CHAIN_OF_RESPONSIBILITY("Chain of Responsibility", PatternFamily.BEHAVIORAL, false, "CCCCC", null),

    /** Evalúa sentencias de un lenguaje propio. */
    INTERPRETER("Interpreter", PatternFamily.BEHAVIORAL, false, "CCCCC", null),

    /** Recorre una colección sin exponer su estructura. */
    ITERATOR("Iterator", PatternFamily.BEHAVIORAL, false, "CCCCC", null),

    /** Objeto central que coordina a otros; como command bus. */
    MEDIATOR("Mediator (command bus)", PatternFamily.BEHAVIORAL, true, "XCCNC",
            "en capas un command bus sustituye a la capa de servicio y añade saltos sin aportar"),

    /** Guarda y restaura el estado de un objeto. */
    MEMENTO("Memento", PatternFamily.BEHAVIORAL, false, "CCCCC", null),

    /** Notifica a los interesados cuando algo cambia. */
    OBSERVER("Observer", PatternFamily.BEHAVIORAL, false, "CCCCC", null),

    /** El comportamiento cambia según el estado del objeto. */
    STATE("State", PatternFamily.BEHAVIORAL, false, "CCCCC", null),

    /** Algoritmos intercambiables detrás de una interfaz. */
    STRATEGY("Strategy", PatternFamily.BEHAVIORAL, false, "CCCCC", null),

    /** Esqueleto de algoritmo con pasos que redefinen las subclases. */
    TEMPLATE_METHOD("Template Method", PatternFamily.BEHAVIORAL, false, "CCCCC", null),

    /** Operaciones nuevas sobre una estructura sin cambiar sus clases. */
    VISITOR("Visitor", PatternFamily.BEHAVIORAL, false, "CCCCC", null),

    // ── Distribuidos e integración ──────────────────────────────────

    /** Publica eventos de forma fiable guardándolos en la misma transacción. */
    TRANSACTIONAL_OUTBOX("Transactional Outbox", PatternFamily.DISTRIBUTED, false, "-CCCN",
            "en capas no hay eventos entre módulos que publicar"),

    /** Transacción larga entre servicios mediante pasos y compensaciones. */
    SAGA("Saga", PatternFamily.DISTRIBUTED, false, "----C",
            "coordina transacciones entre servicios distintos; solo tiene sentido entre módulos o servicios"),

    /** Entrada única que enruta hacia varios servicios. */
    API_GATEWAY("API Gateway / BFF", PatternFamily.DISTRIBUTED, false, "-----",
            "enruta hacia varios servicios; aquí se genera un único proyecto"),

    /** Corta las llamadas a un servicio que falla y reintenta con control. */
    CIRCUIT_BREAKER("Circuit Breaker / Retry", PatternFamily.DISTRIBUTED, false, "CCCCC", null);

    /** Número de columnas de la matriz: una por grupo de estilos. */
    private static final int MATRIX_COLUMNS = 5;

    /** Nombre legible. */
    @Getter
    private final String label;

    /** Catálogo de origen. */
    @Getter
    private final PatternFamily family;

    /**
     * Si el generador escribe código para él; si no, solo se documenta en el
     * README y el ADR. Marca provisional: se ajusta en M9.
     */
    @Getter
    private final boolean generatesCode;

    /** Por qué no se puede elegir en algunos estilos, o {@code null} si se puede en todos. */
    @Getter
    private final String reason;

    /** Un símbolo de compatibilidad por columna de estilo. */
    private final String matrix;

    /**
     * Declara un patrón y valida su matriz.
     *
     * @param label         nombre legible
     * @param family        catálogo de origen
     * @param generatesCode si el generador escribe código para él
     * @param matrix        cinco símbolos de compatibilidad
     * @param reason        por qué se rechaza donde no se puede elegir
     * @throws IllegalArgumentException si la matriz no tiene cinco símbolos válidos
     */
    DesignPattern(final String label, final PatternFamily family, final boolean generatesCode,
                  final String matrix, final String reason) {
        if (matrix.length() != MATRIX_COLUMNS) {
            throw new IllegalArgumentException("La matriz de " + label + " debe tener " + MATRIX_COLUMNS
                    + " símbolos: " + matrix);
        }
        matrix.chars().forEach(symbol -> Compatibility.fromSymbol((char) symbol));
        this.label = label;
        this.family = family;
        this.generatesCode = generatesCode;
        this.matrix = matrix;
        this.reason = reason;
    }

    /**
     * Compatibilidad de este patrón con un estilo.
     *
     * @param style estilo de arquitectura
     * @return relación entre ambos
     */
    public Compatibility compatibilityWith(final ArchitectureStyle style) {
        return Compatibility.fromSymbol(matrix.charAt(style.getMatrixColumn()));
    }

    /**
     * Patrones que un estilo exige.
     *
     * @param style estilo de arquitectura
     * @return conjunto modificable, en el orden del catálogo
     */
    public static Set<DesignPattern> nativeTo(final ArchitectureStyle style) {
        Set<DesignPattern> result = EnumSet.noneOf(DesignPattern.class);
        for (DesignPattern pattern : values()) {
            if (pattern.compatibilityWith(style) == Compatibility.NATIVE) {
                result.add(pattern);
            }
        }
        return result;
    }
}