package com.alumniintelligence.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Deterministic stub implementation of {@link PhantomBusterClient}.
 *
 * <p>Active when a real {@link PhantomBusterClientImpl} bean is not present
 * (i.e., when {@code PHANTOMBUSTER_API_KEY} is not configured).
 *
 * <p>Returns a fixed list of realistic-looking test profiles.  This allows the
 * full API and service layer to be exercised in tests and in local development
 * without real PhantomBuster credentials.
 *
 * <p>All returned profile URLs are fictional and will not resolve.
 */
@Component
@Primary
@ConditionalOnMissingBean(PhantomBusterClientImpl.class)
@Slf4j
public class MockPhantomBusterClient implements PhantomBusterClient {

    @Override
    public List<PhantomBusterProfile> searchAlumni(
            String university, String designation, Integer passoutYear) {

        log.warn("MockPhantomBusterClient is active — returning test fixtures. "
                + "Configure PHANTOMBUSTER_API_KEY to use the real client.");

        return List.of(
                new PhantomBusterProfile(
                        "John Doe",
                        designation,
                        "Acme Corp",
                        university,
                        "New York, NY",
                        "Passionate " + designation + " at Acme Corp",
                        "https://www.linkedin.com/in/johndoe-mock-001",
                        passoutYear != null ? passoutYear : 2020
                ),
                new PhantomBusterProfile(
                        "Jane Smith",
                        designation,
                        "TechCo Ltd",
                        university,
                        "San Francisco, CA",
                        designation + " | Tech Enthusiast",
                        "https://www.linkedin.com/in/janesmith-mock-002",
                        passoutYear != null ? passoutYear : 2019
                ),
                new PhantomBusterProfile(
                        "Alice Johnson",
                        designation,
                        "StartupXYZ",
                        university,
                        "Austin, TX",
                        "Building the future | " + designation,
                        "https://www.linkedin.com/in/alicejohnson-mock-003",
                        passoutYear != null ? passoutYear : 2021
                )
        );
    }
}
