package com.alumniintelligence.client;

import java.util.List;

/**
 * Abstraction over PhantomBuster's agent launch + result polling cycle.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link PhantomBusterClientImpl} — real HTTP calls to the PhantomBuster REST API.
 *   <li>{@link MockPhantomBusterClient} — deterministic stub for unit/integration tests.
 * </ul>
 *
 * <p>The interface is intentionally simple: callers pass the search parameters
 * and receive a list of raw profile data maps. Parsing is the implementation's
 * responsibility.
 */
public interface PhantomBusterClient {

    /**
     * Triggers a LinkedIn alumni search via PhantomBuster and waits (polls) for
     * the results synchronously.
     *
     * @param university   the university name to search
     * @param designation  the current job designation to search
     * @param passoutYear  optional graduation year; {@code null} means no year filter
     * @return a list of raw profile data entries (may be empty, never null)
     * @throws PhantomBusterException if the external call fails or times out
     */
    List<PhantomBusterProfile> searchAlumni(String university, String designation, Integer passoutYear);
}
