package org.jers.apigenerator.infrastructure.version;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

/**
 * Crea el cliente HTTP hacia el repositorio Maven y el reloj del sistema.
 */
@Configuration
public class MavenRepositoryClientConfig {

    /**
     * Cliente HTTP con tiempos límite, para que un repositorio lento no
     * bloquee la aplicación.
     *
     * @param repositoryUrl  URL base del repositorio Maven
     * @param connectTimeout tiempo máximo para conectar
     * @param readTimeout    tiempo máximo para recibir la respuesta
     * @return cliente apuntando al repositorio
     */
    @Bean
    public RestClient mavenRepositoryClient(
            @Value("${app.versions.repository-url}") final String repositoryUrl,
            @Value("${app.versions.connect-timeout}") final Duration connectTimeout,
            @Value("${app.versions.read-timeout}") final Duration readTimeout) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(http);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder()
                .baseUrl(repositoryUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Reloj del sistema en UTC; se inyecta para que los tests controlen el tiempo.
     *
     * @return reloj del sistema
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}