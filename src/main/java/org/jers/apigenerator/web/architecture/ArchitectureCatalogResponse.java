package org.jers.apigenerator.web.architecture;

import org.jers.apigenerator.core.architecture.*;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Respuesta de {@code GET /api/v1/architectures}: estilos, repartos en
 * módulos y todos los patrones con su compatibilidad por estilo, incluidos
 * los que no se pueden elegir, para que la interfaz los muestre marcados.
 *
 * @param styles   estilos de arquitectura, en el orden en que se muestran
 * @param layouts  repartos en módulos
 * @param patterns patrones de diseño, en el orden del catálogo
 */
public record ArchitectureCatalogResponse(List<Style> styles, List<Layout> layouts, List<Pattern> patterns) {

    /**
     * Construye la respuesta con los catálogos del núcleo.
     *
     * @return catálogo completo
     */
    public static ArchitectureCatalogResponse current() {
        return new ArchitectureCatalogResponse(
                Arrays.stream(ArchitectureStyle.values()).map(Style::from).toList(),
                Arrays.stream(ModuleLayout.values()).map(Layout::from).toList(),
                Arrays.stream(DesignPattern.values()).map(Pattern::from).toList());
    }

    /**
     * Estilo de arquitectura.
     *
     * @param id          identificador
     * @param label       nombre legible
     * @param description explicación breve
     * @param multiModule si admite varios módulos de build
     */
    public record Style(ArchitectureStyle id, String label, String description, boolean multiModule) {

        /**
         * Convierte un estilo del núcleo.
         *
         * @param style estilo
         * @return representación para la API
         */
        static Style from(final ArchitectureStyle style) {
            return new Style(style, style.getLabel(), style.getDescription(), style.isMultiModule());
        }
    }

    /**
     * Reparto en módulos.
     *
     * @param id    identificador
     * @param label nombre legible
     */
    public record Layout(ModuleLayout id, String label) {

        /**
         * Convierte un reparto del núcleo.
         *
         * @param layout reparto
         * @return representación para la API
         */
        static Layout from(final ModuleLayout layout) {
            return new Layout(layout, layout.getLabel());
        }
    }

    /**
     * Patrón de diseño con su compatibilidad por estilo.
     *
     * @param id            identificador
     * @param label         nombre legible
     * @param family        catálogo de origen
     * @param familyLabel   nombre legible del catálogo
     * @param generatesCode si genera código o solo se documenta
     * @param reason        por qué no se puede elegir en algunos estilos, o {@code null}
     * @param compatibility relación con cada estilo
     */
    public record Pattern(
            DesignPattern id,
            String label,
            PatternFamily family,
            String familyLabel,
            boolean generatesCode,
            String reason,
            Map<ArchitectureStyle, Compatibility> compatibility) {

        /**
         * Convierte un patrón del núcleo.
         *
         * @param pattern patrón
         * @return representación para la API
         */
        static Pattern from(final DesignPattern pattern) {
            Map<ArchitectureStyle, Compatibility> compatibility = new EnumMap<>(ArchitectureStyle.class);
            for (ArchitectureStyle style : ArchitectureStyle.values()) {
                compatibility.put(style, pattern.compatibilityWith(style));
            }
            return new Pattern(pattern, pattern.getLabel(), pattern.getFamily(), pattern.getFamily().getLabel(),
                    pattern.isGeneratesCode(), pattern.getReason(), compatibility);
        }
    }
}