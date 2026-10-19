package com.alumniintelligence.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Registers application-wide infrastructure beans.
 *
 * <p>Uses the JDK {@link HttpClient} for PhantomBuster HTTP calls to avoid
 * adding an unnecessary dependency on Feign or RestTemplate.
 */
@Configuration
@EnableConfigurationProperties(PhantomBusterProperties.class)
public class AppConfig {

    @Bean
    public HttpClient phantomBusterHttpClient(PhantomBusterProperties props) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.connectTimeoutMs()))
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
}
