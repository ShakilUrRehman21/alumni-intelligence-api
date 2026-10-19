package com.alumniintelligence.config;

import com.alumniintelligence.client.MockPhantomBusterClient;
import com.alumniintelligence.client.PhantomBusterClient;
import com.alumniintelligence.client.PhantomBusterClientImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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

    @Bean
    @ConditionalOnExpression("!'${phantombuster.api-key:}'.trim().isEmpty()")
    public PhantomBusterClient realPhantomBusterClient(
            PhantomBusterProperties props, HttpClient httpClient, ObjectMapper objectMapper) {
        return new PhantomBusterClientImpl(props, httpClient, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(PhantomBusterClient.class)
    public PhantomBusterClient mockPhantomBusterClient() {
        return new MockPhantomBusterClient();
    }
}
