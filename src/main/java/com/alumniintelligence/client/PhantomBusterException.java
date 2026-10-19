package com.alumniintelligence.client;

/**
 * Thrown when a PhantomBuster API call fails for any reason:
 * authentication errors, rate limits, timeouts, or unexpected response formats.
 *
 * <p>Callers should treat this as a recoverable infrastructure failure and
 * mark the related {@link com.alumniintelligence.entity.SearchJob} as FAILED.
 */
public class PhantomBusterException extends RuntimeException {

    public PhantomBusterException(String message) {
        super(message);
    }

    public PhantomBusterException(String message, Throwable cause) {
        super(message, cause);
    }
}
