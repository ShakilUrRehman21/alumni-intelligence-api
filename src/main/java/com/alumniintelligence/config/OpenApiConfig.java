package com.alumniintelligence.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc OpenAPI customization — sets metadata for the Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI alumniIntelligenceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Alumni Intelligence API")
                        .version("1.0.0")
                        .description("""
                                REST API to search and persist LinkedIn alumni profiles
                                using the PhantomBuster integration.

                                All endpoints return a consistent JSON envelope.
                                Authentication is out of scope for this assignment.
                                """)
                        .contact(new Contact()
                                .name("Alumni Intelligence Team"))
                        .license(new License()
                                .name("MIT")));
    }
}
