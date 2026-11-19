package org.jers.apigenerator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.List;

/**
 * Configura Spring MVC para servir la aplicación Angular empaquetada.
 *
 * <p>Los archivos que existen en {@code classpath:/static/} se entregan tal
 * cual (JS, CSS, imágenes). Cualquier otra ruta que no pertenezca a la API
 * ni a la documentación OpenAPI se responde con {@code index.html}, para que
 * el enrutador de Angular la resuelva en el navegador.</p>
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    /** Ubicación en el classpath donde Angular deja su build. */
    private static final String STATIC_LOCATION = "classpath:/static/";

    /** Patrón que captura todas las rutas no atendidas por un controlador. */
    private static final String ALL_PATHS = "/**";

    /** Página de entrada de la aplicación Angular. */
    private static final String INDEX_FILE = "index.html";

    /**
     * Prefijos que nunca deben devolver {@code index.html}: si no existen,
     * deben responder 404 para que el cliente detecte el error.
     */
    private static final List<String> EXCLUDED_PREFIXES =
            List.of("api/", "v3/", "swagger-ui");

    /**
     * Registra el manejador de recursos estáticos con resolución de respaldo
     * hacia {@code index.html}.
     *
     * @param registry registro de manejadores de recursos de Spring MVC
     */
    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler(ALL_PATHS)
                .addResourceLocations(STATIC_LOCATION)
                .resourceChain(true)
                .addResolver(new SpaFallbackResolver());
    }

    /**
     * Resolvedor que entrega el recurso pedido si existe y, en caso
     * contrario, la página {@code index.html} de Angular.
     */
    private static final class SpaFallbackResolver extends PathResourceResolver {

        /**
         * Busca el recurso solicitado y aplica la regla de respaldo.
         *
         * @param resourcePath ruta relativa pedida por el navegador
         * @param location     ubicación base de los recursos estáticos
         * @return el recurso encontrado, {@code index.html} como respaldo, o
         *         {@code null} si la ruta está excluida y no existe
         * @throws IOException si falla el acceso al recurso
         */
        @Override
        protected Resource getResource(final String resourcePath, final Resource location)
                throws IOException {
            Resource requested = location.createRelative(resourcePath);
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }
            if (isExcluded(resourcePath)) {
                return null;
            }
            Resource index = location.createRelative(INDEX_FILE);
            return index.exists() ? index : null;
        }

        /**
         * Indica si la ruta pertenece a la API o a la documentación OpenAPI.
         *
         * @param resourcePath ruta relativa pedida por el navegador
         * @return {@code true} si la ruta no debe recibir {@code index.html}
         */
        private static boolean isExcluded(final String resourcePath) {
            return EXCLUDED_PREFIXES.stream().anyMatch(resourcePath::startsWith);
        }
    }
}