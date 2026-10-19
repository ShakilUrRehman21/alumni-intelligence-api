package com.alumniintelligence.client;

import com.alumniintelligence.config.PhantomBusterProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Real PhantomBuster HTTP client.
 *
 * <p>Activated only when {@code phantombuster.api-key} is non-blank.
 * Uses the JDK {@link HttpClient} to call the PhantomBuster v2 REST API:
 *
 * <ol>
 *   <li>POST /agents/launch — starts the LinkedIn Search Export Phantom.
 *   <li>GET  /agents/fetch-output — polls until the agent finishes or times out.
 *   <li>Parses the CSV/JSON result rows into {@link PhantomBusterProfile} records.
 * </ol>
 *
 * <p><strong>Credentials required:</strong>
 * <ul>
 *   <li>{@code PHANTOMBUSTER_API_KEY} env var — workspace API key from PhantomBuster settings
 *   <li>{@code PHANTOMBUSTER_AGENT_ID} env var — the agent's numeric ID
 *   <li>{@code LINKEDIN_SESSION_COOKIE} env var — the {@code li_at} cookie value
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "phantombuster.api-key", matchIfMissing = false, havingValue = "")
public class PhantomBusterClientImpl implements PhantomBusterClient {

    private static final String HEADER_KEY   = "X-Phantombuster-Key";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String JSON_MIME    = "application/json";

    private final PhantomBusterProperties props;
    private final HttpClient              httpClient;
    private final ObjectMapper            objectMapper;

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    @Override
    public List<PhantomBusterProfile> searchAlumni(
            String university, String designation, Integer passoutYear) {

        validateCredentials();

        String searchUrl = buildLinkedInSearchUrl(university, designation, passoutYear);
        log.info("Launching PhantomBuster agent {} with search URL: {}", props.agentId(), searchUrl);

        String containerId = launchAgent(searchUrl);
        log.info("PhantomBuster container launched: {}", containerId);

        JsonNode output = pollForOutput(containerId);
        log.info("PhantomBuster agent {} completed", props.agentId());

        return parseOutput(output, university);
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private void validateCredentials() {
        if (isBlank(props.apiKey())) {
            throw new PhantomBusterException(
                    "PHANTOMBUSTER_API_KEY is not configured. Set it as an environment variable.");
        }
        if (isBlank(props.agentId())) {
            throw new PhantomBusterException(
                    "PHANTOMBUSTER_AGENT_ID is not configured. Set it as an environment variable.");
        }
        if (isBlank(props.linkedinSessionCookie())) {
            throw new PhantomBusterException(
                    "LINKEDIN_SESSION_COOKIE is not configured. Set it as an environment variable.");
        }
    }

    /**
     * Builds a LinkedIn people-search URL from the provided inputs.
     * Example: https://www.linkedin.com/search/results/people/?keywords=Software+Engineer+University+of+XYZ+2020
     */
    private String buildLinkedInSearchUrl(String university, String designation, Integer passoutYear) {
        StringBuilder keywords = new StringBuilder();
        keywords.append(designation).append(" ").append(university);
        if (passoutYear != null) {
            keywords.append(" ").append(passoutYear);
        }
        String encoded = URLEncoder.encode(keywords.toString(), StandardCharsets.UTF_8);
        return "https://www.linkedin.com/search/results/people/?keywords=" + encoded;
    }

    /** Calls POST /agents/launch and returns the container ID. */
    private String launchAgent(String searchUrl) {
        try {
            String body = objectMapper.writeValueAsString(new LaunchRequest(
                    props.agentId(),
                    new LaunchArgument(props.linkedinSessionCookie(), searchUrl)
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.baseUrl() + "/agents/launch"))
                    .header(HEADER_KEY, props.apiKey())
                    .header(CONTENT_TYPE, JSON_MIME)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofMillis(props.readTimeoutMs()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            handleHttpError(response, "launch agent");

            JsonNode json = objectMapper.readTree(response.body());
            String containerId = json.path("containerId").asText(null);
            if (containerId == null || containerId.isBlank()) {
                throw new PhantomBusterException("PhantomBuster launch response missing containerId: " + response.body());
            }
            return containerId;

        } catch (PhantomBusterException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PhantomBusterException("Failed to launch PhantomBuster agent: " + e.getMessage(), e);
        }
    }

    /** Polls GET /agents/fetch-output until the agent finishes. */
    private JsonNode pollForOutput(String containerId) {
        int attempts = 0;

        while (attempts < props.maxPollAttempts()) {
            attempts++;
            try {
                Thread.sleep(props.pollIntervalMs());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(props.baseUrl() + "/agents/fetch-output?id=" + props.agentId()))
                        .header(HEADER_KEY, props.apiKey())
                        .GET()
                        .timeout(Duration.ofMillis(props.readTimeoutMs()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                handleHttpError(response, "fetch output");

                JsonNode json = objectMapper.readTree(response.body());
                String status = json.path("status").asText("");

                log.debug("Poll attempt {}/{} — status: {}", attempts, props.maxPollAttempts(), status);

                if ("finished".equalsIgnoreCase(status)) {
                    return json.path("output");
                }
                if ("error".equalsIgnoreCase(status)) {
                    String errMsg = json.path("message").asText("Unknown PhantomBuster error");
                    throw new PhantomBusterException("PhantomBuster agent failed: " + errMsg);
                }

            } catch (PhantomBusterException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new PhantomBusterException("Polling interrupted", e);
            } catch (IOException e) {
                log.warn("Poll attempt {} failed: {}", attempts, e.getMessage());
            }
        }

        throw new PhantomBusterException(
                "PhantomBuster agent did not finish within " + props.maxPollAttempts() + " polling attempts");
    }

    /**
     * Parses the output JSON node (which is an array of profile objects from
     * the LinkedIn Search Export Phantom) into {@link PhantomBusterProfile} records.
     */
    private List<PhantomBusterProfile> parseOutput(JsonNode output, String university) {
        List<PhantomBusterProfile> profiles = new ArrayList<>();

        if (output == null || !output.isArray()) {
            log.warn("PhantomBuster output is not an array: {}", output);
            return profiles;
        }

        for (JsonNode node : output) {
            try {
                profiles.add(new PhantomBusterProfile(
                        node.path("fullName").asText(null),
                        node.path("jobTitle").asText(null),
                        node.path("company").asText(null),
                        node.path("school").asText(university),   // fall back to queried university
                        node.path("location").asText(null),
                        node.path("headline").asText(null),
                        node.path("profileUrl").asText(null),
                        parseYear(node.path("graduationYear").asText(null))
                ));
            } catch (Exception e) {
                log.warn("Skipping malformed PhantomBuster profile row: {}", e.getMessage());
            }
        }

        log.info("Parsed {} profiles from PhantomBuster output", profiles.size());
        return profiles;
    }

    private void handleHttpError(HttpResponse<String> response, String operation) {
        int code = response.statusCode();
        if (code == 401 || code == 403) {
            throw new PhantomBusterException(
                    "PhantomBuster authentication failed (HTTP " + code + "). Verify PHANTOMBUSTER_API_KEY.");
        }
        if (code == 429) {
            throw new PhantomBusterException("PhantomBuster rate limit exceeded (HTTP 429). Retry later.");
        }
        if (code >= 400) {
            throw new PhantomBusterException(
                    "PhantomBuster " + operation + " returned HTTP " + code + ": " + response.body());
        }
    }

    private static Integer parseYear(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // ----------------------------------------------------------------
    // Internal request payload records
    // ----------------------------------------------------------------

    private record LaunchRequest(String id, LaunchArgument argument) {}

    private record LaunchArgument(String sessionCookie, String searches) {}
}
