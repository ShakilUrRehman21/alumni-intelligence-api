package com.alumniintelligence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single alumni profile-search request submitted to PhantomBuster.
 *
 * <p>One {@code SearchJob} may result in multiple {@link AlumniProfile} records
 * being persisted. The job tracks the full lifecycle from PENDING → RUNNING →
 * COMPLETED (or FAILED).
 */
@Entity
@Table(name = "search_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String university;

    @Column(nullable = false, length = 255)
    private String designation;

    @Column(name = "passout_year")
    private Integer passoutYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "phantombuster_run_id", length = 255)
    private String phantombusterRunId;

    @Column(name = "result_count")
    @Builder.Default
    private Integer resultCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @OneToMany(mappedBy = "searchJob", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AlumniProfile> profiles = new ArrayList<>();

    // ----------------------------------------------------------------
    // Domain methods
    // ----------------------------------------------------------------

    public void markRunning(String pbRunId) {
        this.status = JobStatus.RUNNING;
        this.phantombusterRunId = pbRunId;
    }

    public void markCompleted(int profileCount) {
        this.status = JobStatus.COMPLETED;
        this.resultCount = profileCount;
        this.completedAt = OffsetDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = JobStatus.FAILED;
        this.errorMessage = reason;
        this.completedAt = OffsetDateTime.now();
    }
}
