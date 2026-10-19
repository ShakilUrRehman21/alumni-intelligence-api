package com.alumniintelligence.controller;

import com.alumniintelligence.dto.request.AlumniSearchRequest;
import com.alumniintelligence.dto.response.AlumniProfileResponse;
import com.alumniintelligence.dto.response.ApiResponse;
import com.alumniintelligence.dto.response.SearchJobResponse;
import com.alumniintelligence.entity.JobStatus;
import com.alumniintelligence.service.AlumniService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for alumni profile search and retrieval.
 *
 * <p>All business logic is delegated to {@link AlumniService}.
 * Controllers only handle HTTP concerns: routing, deserialization, and response codes.
 */
@RestController
@RequestMapping("/api/alumni")
@RequiredArgsConstructor
@Validated
@Tag(name = "Alumni Profiles", description = "Search and retrieve LinkedIn alumni profiles")
public class AlumniController {

    private final AlumniService alumniService;

    // ----------------------------------------------------------------
    // POST /api/alumni/search (Primary assignment endpoint)
    // ----------------------------------------------------------------

    @PostMapping("/search")
    @Operation(
            summary = "Search Alumni Profiles (Assignment Endpoint)",
            description = "Accepts university name, designation, and optional pass-out year, queries PhantomBuster, persists retrieved profiles in PostgreSQL, and returns the list of alumni profiles."
    )
    public ResponseEntity<ApiResponse<List<AlumniProfileResponse>>> searchAlumni(
            @Valid @RequestBody AlumniSearchRequest request) {

        List<AlumniProfileResponse> profiles = alumniService.searchAlumni(request);
        return ResponseEntity.ok(ApiResponse.success(profiles));
    }

    // ----------------------------------------------------------------
    // POST /api/alumni/search/async (Background execution endpoint)
    // ----------------------------------------------------------------

    @PostMapping("/search/async")
    @Operation(
            summary = "Trigger an asynchronous alumni search job",
            description = "Initiates a background PhantomBuster-powered LinkedIn search job. Returns HTTP 202 with the search job ID. Poll GET /api/alumni/jobs/{jobId} for status."
    )
    public ResponseEntity<ApiResponse<SearchJobResponse>> searchAlumniAsync(
            @Valid @RequestBody AlumniSearchRequest request) {

        SearchJobResponse job = alumniService.initiateSearch(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Search job initiated. Check job status for results.", job));
    }

    // ----------------------------------------------------------------
    // GET /api/alumni/all (Assignment endpoint)
    // ----------------------------------------------------------------

    @GetMapping("/all")
    @Operation(
            summary = "Get all saved alumni profiles (Assignment Endpoint)",
            description = "Returns the list of all saved alumni profiles from PostgreSQL."
    )
    public ResponseEntity<ApiResponse<List<AlumniProfileResponse>>> getAllProfiles() {
        return ResponseEntity.ok(ApiResponse.success(alumniService.getAllProfilesList()));
    }

    // ----------------------------------------------------------------
    // GET /api/alumni (Paginated directory)
    // ----------------------------------------------------------------

    @GetMapping
    @Operation(summary = "List saved alumni profiles (Paginated)", description = "Returns paginated list of persisted profiles.")
    public ResponseEntity<ApiResponse<Page<AlumniProfileResponse>>> getPaginatedProfiles(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (max 100)")   @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AlumniProfileResponse> profiles = alumniService.getAllProfiles(pageable);
        return ResponseEntity.ok(ApiResponse.success(profiles));
    }

    // ----------------------------------------------------------------
    // GET /api/alumni/{id}
    // ----------------------------------------------------------------

    @GetMapping("/{id}")
    @Operation(summary = "Get a single alumni profile by ID")
    public ResponseEntity<ApiResponse<AlumniProfileResponse>> getProfileById(
            @PathVariable Long id) {

        return ResponseEntity.ok(ApiResponse.success(alumniService.getProfileById(id)));
    }

    // ----------------------------------------------------------------
    // GET /api/alumni/search  (filter + paginate)
    // ----------------------------------------------------------------

    @GetMapping("/search")
    @Operation(
            summary = "Filter saved alumni profiles",
            description = "Searches the local database for profiles matching the given filters. "
                    + "All filters are optional. Case-insensitive."
    )
    public ResponseEntity<ApiResponse<Page<AlumniProfileResponse>>> searchProfiles(
            @Parameter(description = "University name (case-insensitive exact match)")
            @RequestParam(required = false) String university,

            @Parameter(description = "Current role (case-insensitive substring match)")
            @RequestParam(required = false) String currentRole,

            @Parameter(description = "Graduation year")
            @RequestParam(required = false) @Min(1900) @Max(2100) Integer passoutYear,

            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (max 100)")   @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AlumniProfileResponse> results = alumniService.searchProfiles(university, currentRole, passoutYear, pageable);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    // ----------------------------------------------------------------
    // GET /api/alumni/jobs/{jobId}
    // ----------------------------------------------------------------

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get search job status", description = "Returns current status and metadata for a search job.")
    public ResponseEntity<ApiResponse<SearchJobResponse>> getJobStatus(
            @PathVariable Long jobId) {

        return ResponseEntity.ok(ApiResponse.success(alumniService.getJobById(jobId)));
    }

    // ----------------------------------------------------------------
    // GET /api/alumni/jobs  (paginated job history)
    // ----------------------------------------------------------------

    @GetMapping("/jobs")
    @Operation(summary = "List search jobs", description = "Paginated list of all search jobs, optionally filtered by status.")
    public ResponseEntity<ApiResponse<Page<SearchJobResponse>>> listJobs(
            @Parameter(description = "Filter by job status")
            @RequestParam(required = false) JobStatus status,

            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(alumniService.getAllJobs(status, pageable)));
    }
}
