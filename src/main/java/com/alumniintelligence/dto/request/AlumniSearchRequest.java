package com.alumniintelligence.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Request body for {@code POST /api/alumni/search}.
 *
 * <p>{@code passoutYear} is optional; when provided it narrows the LinkedIn
 * search query to alumni from a specific graduation year.
 */
@Schema(description = "Alumni search request parameters")
public record AlumniSearchRequest(

        @Schema(description = "Name of the university", example = "University of XYZ")
        @NotBlank(message = "university must not be blank")
        @Size(max = 255, message = "university must not exceed 255 characters")
        String university,

        @Schema(description = "Current professional designation", example = "Software Engineer")
        @NotBlank(message = "designation must not be blank")
        @Size(max = 255, message = "designation must not exceed 255 characters")
        String designation,

        @Schema(description = "Optional graduation year", example = "2020")
        @Min(value = 1900, message = "passoutYear must be 1900 or later")
        @Max(value = 2100, message = "passoutYear must be 2100 or earlier")
        Integer passoutYear
) {}
