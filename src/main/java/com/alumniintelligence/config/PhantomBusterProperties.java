package com.alumniintelligence.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Strongly-typed binding for the {@code phantombuster.*} properties namespace.
 *
 * <p>Loaded from {@code application.yml}. All credentials must be supplied via
 * environment variables; they are never hard-coded here.
 */
@ConfigurationProperties(prefix = "phantombuster")
@Validated
public record PhantomBusterProperties(

        /** PhantomBuster workspace API key — set via PHANTOMBUSTER_API_KEY env var. */
        String apiKey,

        /** ID of the pre-configured LinkedIn Search Export Phantom agent. */
        String agentId,

        /** LinkedIn li_at session cookie — set via LINKEDIN_SESSION_COOKIE env var. */
        String linkedinSessionCookie,

        /** Base URL of the PhantomBuster REST API. */
        @NotBlank String baseUrl,

        /** Connection timeout in milliseconds. */
        @Positive int connectTimeoutMs,

        /** Read timeout in milliseconds. */
        @Positive int readTimeoutMs,

        /** Milliseconds between consecutive status-poll requests. */
        @Positive int pollIntervalMs,

        /** Maximum number of polling attempts before giving up. */
        @Positive int maxPollAttempts
) {}
