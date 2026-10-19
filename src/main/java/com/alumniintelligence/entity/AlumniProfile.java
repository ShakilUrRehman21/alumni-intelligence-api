package com.alumniintelligence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Persisted LinkedIn alumni profile record.
 *
 * <p>The {@code linkedinProfileUrl} column is the deduplication key: a UNIQUE
 * constraint on the database level prevents duplicate records. When a search
 * returns a profile whose URL already exists, the existing row is updated rather
 * than a new row being inserted.
 */
@Entity
@Table(
        name = "alumni_profiles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_alumni_linkedin_url", columnNames = "linkedin_profile_url")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlumniProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String name;

    @Column(name = "current_role", length = 255)
    private String currentRole;

    @Column(nullable = false, length = 255)
    private String university;

    @Column(length = 255)
    private String location;

    @Column(name = "linkedin_headline", columnDefinition = "TEXT")
    private String linkedinHeadline;

    @Column(name = "linkedin_profile_url", length = 500, unique = true)
    private String linkedinProfileUrl;

    @Column(name = "passout_year")
    private Integer passoutYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "search_job_id", foreignKey = @ForeignKey(name = "fk_alumni_search_job"))
    private SearchJob searchJob;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
