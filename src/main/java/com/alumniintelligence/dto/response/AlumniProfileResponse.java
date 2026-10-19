package com.alumniintelligence.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * Response DTO representing a single persisted alumni profile.
 *
 * <p>Matches the structure specified in the assignment brief.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Alumni profile data")
public record AlumniProfileResponse(

        @Schema(description = "Internal database ID")
        Long id,

        @Schema(description = "Full name of the alumni", example = "John Doe")
        String name,

        @Schema(description = "Current job role", example = "Software Engineer")
        String currentRole,

        @Schema(description = "University attended", example = "University of XYZ")
        String university,

        @Schema(description = "Current location", example = "New York, NY")
        String location,

        @Schema(description = "LinkedIn headline")
        String linkedinHeadline,

        @Schema(description = "LinkedIn profile URL")
        String linkedinProfileUrl,

        @Schema(description = "Year of graduation", example = "2020")
        Integer passoutYear,

        @Schema(description = "ID of the search job that found this profile")
        Long searchJobId,

        @Schema(description = "Record creation timestamp")
        OffsetDateTime createdAt
) {}
