package com.alumniintelligence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the Alumni Intelligence API.
 *
 * <p>This application exposes REST endpoints to search LinkedIn alumni profiles
 * via PhantomBuster and persist them in PostgreSQL.
 */
@SpringBootApplication
@EnableAsync
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class AlumniIntelligenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlumniIntelligenceApplication.class, args);
    }
}
