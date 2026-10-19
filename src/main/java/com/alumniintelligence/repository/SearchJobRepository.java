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

    /** Paginated job history. */
    Page<SearchJob> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
