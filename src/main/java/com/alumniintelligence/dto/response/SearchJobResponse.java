package com.alumniintelligence.dto.response;

import com.alumniintelligence.entity.JobStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * Response DTO for a search job, returned by POST /api/alumni/search and
 * GET /api/alumni/jobs/{jobId}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Search job metadata and current status")
public record SearchJobResponse(

        @Schema(description = "Search job ID")
        Long id,

        @Schema(description = "University queried")
        String university,

        @Schema(description = "Designation queried")
        String designation,

        @Schema(description = "Optional graduation year filter")
        Integer passoutYear,

        @Schema(description = "Current job status")
        JobStatus status,

        @Schema(description = "Number of profiles retrieved")
        Integer resultCount,

        @Schema(description = "Error description if status is FAILED")
        String errorMessage,

        @Schema(description = "Job creation timestamp")
        OffsetDateTime createdAt,

        @Schema(description = "Job completion timestamp")
        OffsetDateTime completedAt
) {}
