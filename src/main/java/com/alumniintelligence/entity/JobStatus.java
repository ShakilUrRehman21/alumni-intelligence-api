package com.alumniintelligence.entity;

/**
 * Lifecycle states for a {@link SearchJob}.
 */
public enum JobStatus {
    /** Created but not yet submitted to PhantomBuster. */
    PENDING,
    /** Submitted to PhantomBuster and awaiting results. */
    RUNNING,
    /** PhantomBuster returned results and they have been persisted. */
    COMPLETED,
    /** PhantomBuster returned an error or polling timed out. */
    FAILED
}
