package com.alumniintelligence.repository;

import com.alumniintelligence.entity.JobStatus;
import com.alumniintelligence.entity.SearchJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link SearchJob}.
 */
public interface SearchJobRepository extends JpaRepository<SearchJob, Long> {

    /** Lists jobs in a specific status, ordered newest-first. */
    List<SearchJob> findByStatusOrderByCreatedAtDesc(JobStatus status);

    /** Finds jobs matching any of the given statuses (e.g., PENDING or RUNNING for crash recovery). */
    List<SearchJob> findByStatusIn(java.util.Collection<JobStatus> statuses);

    /** Paginated job history. */
    Page<SearchJob> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
