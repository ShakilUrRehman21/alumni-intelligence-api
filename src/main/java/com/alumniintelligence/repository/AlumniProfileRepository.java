package com.alumniintelligence.repository;

import com.alumniintelligence.entity.AlumniProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link AlumniProfile}.
 *
 * <p>All queries go through JPQL rather than native SQL so that the dialect
 * mapping is database-agnostic in tests (Testcontainers PostgreSQL).
 */
public interface AlumniProfileRepository extends JpaRepository<AlumniProfile, Long> {

    /**
     * Finds a profile by its canonical LinkedIn URL.
     * Used for upsert deduplication logic.
     */
    Optional<AlumniProfile> findByLinkedinProfileUrl(String linkedinProfileUrl);

    /**
     * Checks whether a profile with the given LinkedIn URL already exists.
     */
    boolean existsByLinkedinProfileUrl(String linkedinProfileUrl);

    /**
     * Paginated search by university (case-insensitive) and optional designation / year.
     *
     * <p>All nullable params are excluded from the WHERE clause when {@code NULL}.
     */
    @Query("""
            SELECT a FROM AlumniProfile a
            WHERE (CAST(:university AS string) IS NULL OR LOWER(a.university) = LOWER(CAST(:university AS string)))
              AND (CAST(:currentRole AS string) IS NULL OR LOWER(a.currentRole) LIKE LOWER(CONCAT('%', CAST(:currentRole AS string), '%')))
              AND (:passoutYear IS NULL OR a.passoutYear = :passoutYear)
            """)
    Page<AlumniProfile> findByFilters(
            @Param("university")  String university,
            @Param("currentRole") String currentRole,
            @Param("passoutYear") Integer passoutYear,
            Pageable pageable
    );
}
