package com.alumniintelligence.service;

import com.alumniintelligence.client.PhantomBusterClient;
import com.alumniintelligence.client.PhantomBusterException;
import com.alumniintelligence.client.PhantomBusterProfile;
import com.alumniintelligence.dto.request.AlumniSearchRequest;
import com.alumniintelligence.dto.response.AlumniProfileResponse;
import com.alumniintelligence.dto.response.SearchJobResponse;
import com.alumniintelligence.entity.AlumniProfile;
import com.alumniintelligence.entity.JobStatus;
import com.alumniintelligence.entity.SearchJob;
import com.alumniintelligence.exception.ResourceNotFoundException;
import com.alumniintelligence.mapper.AlumniMapper;
import com.alumniintelligence.repository.AlumniProfileRepository;
import com.alumniintelligence.repository.SearchJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Core business logic for alumni search and profile persistence.
 *
 * <p>The {@link #initiateSearch} method creates a {@link SearchJob} and hands
 * the actual PhantomBuster call and persistence off to {@link #executeSearch},
 * which runs asynchronously.  This keeps the HTTP response time short.
 *
 * <p>Deduplication strategy: {@code linkedin_profile_url} is the natural key.
 * If a profile URL already exists in the database, the existing row is updated
 * rather than creating a duplicate.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlumniService {

    private final AlumniProfileRepository profileRepository;
    private final SearchJobRepository     jobRepository;
    private final PhantomBusterClient     phantomBusterClient;
    private final AlumniMapper            mapper;

    // ----------------------------------------------------------------
    // ----------------------------------------------------------------
    // Synchronous search (Primary assignment requirement)
    // ----------------------------------------------------------------

    /**
     * Executes the alumni search synchronously: triggers PhantomBuster,
     * persists/deduplicates profiles in PostgreSQL, records the SearchJob,
     * and returns the list of matching alumni profiles.
     */
    @Transactional
    public List<AlumniProfileResponse> searchAlumni(AlumniSearchRequest request) {
        SearchJob job = SearchJob.builder()
                .university(request.university().strip())
                .designation(request.designation().strip())
                .passoutYear(request.passoutYear())
                .build();
        job.markRunning(null);
        job = jobRepository.save(job);

        try {
            List<PhantomBusterProfile> rawProfiles = phantomBusterClient.searchAlumni(
                    job.getUniversity(),
                    job.getDesignation(),
                    job.getPassoutYear()
            );

            List<AlumniProfile> savedEntities = persistAndReturnProfiles(rawProfiles, job);
            job.markCompleted(savedEntities.size());
            jobRepository.save(job);

            return savedEntities.stream()
                    .map(mapper::toProfileResponse)
                    .toList();

        } catch (PhantomBusterException e) {
            job.markFailed(e.getMessage());
            jobRepository.save(job);
            throw e;
        } catch (Exception e) {
            job.markFailed(e.getMessage());
            jobRepository.save(job);
            throw new PhantomBusterException("Search execution failed: " + e.getMessage(), e);
        }
    }

    // ----------------------------------------------------------------
    // Asynchronous search initiation (Optional background job API)
    // ----------------------------------------------------------------

    /**
     * Creates a {@link SearchJob} in PENDING state, triggers async execution,
     * and immediately returns the job response (HTTP 202).
     */
    @Transactional
    public SearchJobResponse initiateSearch(AlumniSearchRequest request) {
        SearchJob job = SearchJob.builder()
                .university(request.university().strip())
                .designation(request.designation().strip())
                .passoutYear(request.passoutYear())
                .build();

        job = jobRepository.save(job);
        log.info("Created async search job {} for university='{}', designation='{}'",
                job.getId(), job.getUniversity(), job.getDesignation());

        executeSearch(job.getId());
        return mapper.toJobResponse(job);
    }

    // ----------------------------------------------------------------
    // Async execution
    // ----------------------------------------------------------------

    /**
     * Performs the PhantomBuster call and persists results.
     * Runs on the {@code searchTaskExecutor} thread pool.
     */
    @Async("searchTaskExecutor")
    @Transactional
    public void executeSearch(Long jobId) {
        SearchJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("SearchJob not found: " + jobId));

        job.markRunning(null);
        jobRepository.save(job);

        try {
            List<PhantomBusterProfile> rawProfiles = phantomBusterClient.searchAlumni(
                    job.getUniversity(),
                    job.getDesignation(),
                    job.getPassoutYear()
            );

            int saved = persistProfiles(rawProfiles, job);
            job.markCompleted(saved);
            log.info("Search job {} completed — {} profiles saved/updated", jobId, saved);

        } catch (PhantomBusterException e) {
            log.error("Search job {} failed: {}", jobId, e.getMessage());
            job.markFailed(e.getMessage());
        } catch (Exception e) {
            log.error("Search job {} failed with unexpected error", jobId, e);
            job.markFailed("Unexpected error: " + e.getMessage());
        } finally {
            jobRepository.save(job);
        }
    }

    // ----------------------------------------------------------------
    // Profile persistence (upsert)
    // ----------------------------------------------------------------

    private List<AlumniProfile> persistAndReturnProfiles(List<PhantomBusterProfile> rawProfiles, SearchJob job) {
        List<AlumniProfile> saved = new java.util.ArrayList<>();
        for (PhantomBusterProfile raw : rawProfiles) {
            try {
                AlumniProfile profile = upsertAndReturnProfile(raw, job);
                if (profile != null) {
                    saved.add(profile);
                }
            } catch (Exception e) {
                log.warn("Failed to persist profile '{}': {}", raw.fullName(), e.getMessage());
            }
        }
        return saved;
    }

    private int persistProfiles(List<PhantomBusterProfile> rawProfiles, SearchJob job) {
        return persistAndReturnProfiles(rawProfiles, job).size();
    }

    private AlumniProfile upsertAndReturnProfile(PhantomBusterProfile raw, SearchJob job) {
        String url = normalizeUrl(raw.profileUrl());

        AlumniProfile profile;
        if (url != null && !url.isBlank()) {
            profile = profileRepository.findByLinkedinProfileUrl(url)
                    .orElse(AlumniProfile.builder().build());
            profile.setLinkedinProfileUrl(url);
        } else {
            profile = AlumniProfile.builder().build();
        }

        profile.setName(raw.fullName());
        profile.setCurrentRole(raw.jobTitle());
        profile.setUniversity(raw.school() != null ? raw.school() : job.getUniversity());
        profile.setLocation(raw.location());
        profile.setLinkedinHeadline(raw.headline());
        profile.setPassoutYear(raw.graduationYear() != null ? raw.graduationYear() : job.getPassoutYear());
        profile.setSearchJob(job);

        return profileRepository.save(profile);
    }

    private boolean upsertProfile(PhantomBusterProfile raw, SearchJob job) {
        return upsertAndReturnProfile(raw, job) != null;
    }

    // ----------------------------------------------------------------
    // Read operations
    // ----------------------------------------------------------------

    /** Retrieves all saved profiles as a list (matching assignment contract). */
    @Transactional(readOnly = true)
    public List<AlumniProfileResponse> getAllProfilesList() {
        return profileRepository.findAll(org.springframework.data.domain.Sort.by("createdAt").descending())
                .stream()
                .map(mapper::toProfileResponse)
                .toList();
    }

    /** Retrieves all profiles with pagination. */
    @Transactional(readOnly = true)
    public Page<AlumniProfileResponse> getAllProfiles(Pageable pageable) {
        return profileRepository.findAll(pageable)
                .map(mapper::toProfileResponse);
    }

    /** Retrieves a single profile by ID. */
    @Transactional(readOnly = true)
    public AlumniProfileResponse getProfileById(Long id) {
        return profileRepository.findById(id)
                .map(mapper::toProfileResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Alumni profile not found: " + id));
    }

    /** Filtered, paginated profile search. */
    @Transactional(readOnly = true)
    public Page<AlumniProfileResponse> searchProfiles(
            String university, String currentRole, Integer passoutYear, Pageable pageable) {
        return profileRepository.findByFilters(university, currentRole, passoutYear, pageable)
                .map(mapper::toProfileResponse);
    }

    /** Returns a job by ID. */
    @Transactional(readOnly = true)
    public SearchJobResponse getJobById(Long id) {
        return jobRepository.findById(id)
                .map(mapper::toJobResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Search job not found: " + id));
    }

    /** Returns all jobs whose status matches the filter (or all if null). */
    @Transactional(readOnly = true)
    public Page<SearchJobResponse> getAllJobs(JobStatus status, Pageable pageable) {
        if (status != null) {
            List<SearchJob> jobs = jobRepository.findByStatusOrderByCreatedAtDesc(status);
            // Wrap in a Page for consistent pagination response
            long total = jobs.size();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), jobs.size());
            List<SearchJobResponse> content = (start > total)
                    ? List.of()
                    : jobs.subList(start, end).stream().map(mapper::toJobResponse).toList();
            return new org.springframework.data.domain.PageImpl<>(content, pageable, total);
        }
        return jobRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(mapper::toJobResponse);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static String normalizeUrl(String url) {
        if (url == null || url.isBlank()) return null;
        // Strip trailing slashes and query strings for canonical form
        String normalized = url.strip();
        int queryIdx = normalized.indexOf('?');
        if (queryIdx > 0) normalized = normalized.substring(0, queryIdx);
        if (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }
}
