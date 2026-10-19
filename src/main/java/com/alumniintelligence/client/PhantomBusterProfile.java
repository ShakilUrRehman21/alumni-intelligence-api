package com.alumniintelligence.client;

/**
 * Raw profile data returned by the PhantomBuster LinkedIn Search Export Phantom.
 *
 * <p>Field names match PhantomBuster's documented output columns.  All fields
 * are nullable because PhantomBuster may omit any field for a given profile.
 */
public record PhantomBusterProfile(

        /** LinkedIn "First Name + Last Name" */
        String fullName,

        /** Job title shown on the profile */
        String jobTitle,

        /** Company name */
        String companyName,

        /** University / school listed on profile */
        String school,

        /** Location text from LinkedIn */
        String location,

        /** LinkedIn headline */
        String headline,

        /** Canonical LinkedIn profile URL */
        String profileUrl,

        /** Graduation year extracted from education section (may be null) */
        Integer graduationYear
) {}
