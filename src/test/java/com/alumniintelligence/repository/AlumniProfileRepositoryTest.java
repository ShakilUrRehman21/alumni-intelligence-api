package com.alumniintelligence.repository;

import com.alumniintelligence.entity.AlumniProfile;
import com.alumniintelligence.entity.SearchJob;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for {@link AlumniProfileRepository} using a real PostgreSQL
 * instance provisioned by Testcontainers.
 *
 * <p>Flyway migrations are applied automatically by Spring Boot's auto-configuration,
 * so the schema matches production exactly.
 *
 * <p>Skipped automatically in environments without a running Docker daemon.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AlumniProfileRepositoryTest {

    @Autowired AlumniProfileRepository profileRepository;
    @Autowired SearchJobRepository      jobRepository;

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private SearchJob savedJob() {
        SearchJob job = SearchJob.builder()
                .university("MIT")
                .designation("Engineer")
                .build();
        return jobRepository.save(job);
    }

    private AlumniProfile profile(String name, String url, String university, SearchJob job) {
        return AlumniProfile.builder()
                .name(name)
                .currentRole("Software Engineer")
                .university(university)
                .location("New York")
                .linkedinProfileUrl(url)
                .passoutYear(2020)
                .searchJob(job)
                .build();
    }

    // ----------------------------------------------------------------
    // Basic CRUD
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Save and retrieve alumni profile by ID")
    void saveAndFindById() {
        SearchJob job = savedJob();
        AlumniProfile p = profileRepository.save(profile("Alice", "https://linkedin.com/in/alice", "MIT", job));

        Optional<AlumniProfile> found = profileRepository.findById(p.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alice");
        assertThat(found.get().getLinkedinProfileUrl()).isEqualTo("https://linkedin.com/in/alice");
    }

    // ----------------------------------------------------------------
    // Deduplication
    // ----------------------------------------------------------------

    @Test
    @DisplayName("existsByLinkedinProfileUrl returns true for existing URL")
    void existsByLinkedinProfileUrl_trueForExistingUrl() {
        SearchJob job = savedJob();
        profileRepository.save(profile("Bob", "https://linkedin.com/in/bob", "MIT", job));

        assertThat(profileRepository.existsByLinkedinProfileUrl("https://linkedin.com/in/bob")).isTrue();
    }

    @Test
    @DisplayName("existsByLinkedinProfileUrl returns false for unknown URL")
    void existsByLinkedinProfileUrl_falseForUnknown() {
        assertThat(profileRepository.existsByLinkedinProfileUrl("https://linkedin.com/in/nobody")).isFalse();
    }

    @Test
    @DisplayName("findByLinkedinProfileUrl returns profile when URL exists")
    void findByLinkedinProfileUrl_returnsProfile() {
        SearchJob job = savedJob();
        profileRepository.save(profile("Carol", "https://linkedin.com/in/carol", "MIT", job));

        Optional<AlumniProfile> found = profileRepository.findByLinkedinProfileUrl("https://linkedin.com/in/carol");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Carol");
    }

    @Test
    @DisplayName("Saving duplicate LinkedIn URL throws DataIntegrityViolationException")
    void duplicateLinkedinUrl_throwsConstraintViolation() {
        SearchJob job = savedJob();
        profileRepository.save(profile("Dave", "https://linkedin.com/in/dave", "MIT", job));

        AlumniProfile duplicate = profile("Dave Clone", "https://linkedin.com/in/dave", "MIT", job);

        assertThatThrownBy(() -> {
            profileRepository.save(duplicate);
            profileRepository.flush(); // force flush to DB to trigger constraint
        }).isInstanceOf(Exception.class); // DataIntegrityViolationException or PersistenceException
    }

    // ----------------------------------------------------------------
    // Filter query
    // ----------------------------------------------------------------

    @Test
    @DisplayName("findByFilters returns profiles matching university (case-insensitive)")
    void findByFilters_university_caseInsensitive() {
        SearchJob job = savedJob();
        profileRepository.save(profile("Eve", "https://linkedin.com/in/eve", "MIT", job));
        profileRepository.save(profile("Frank", "https://linkedin.com/in/frank", "Stanford", job));

        Page<AlumniProfile> results = profileRepository.findByFilters("mit", null, null, PageRequest.of(0, 10));

        assertThat(results.getContent())
                .extracting(AlumniProfile::getName)
                .containsExactly("Eve");
    }

    @Test
    @DisplayName("findByFilters with null filters returns all profiles")
    void findByFilters_nullFilters_returnsAll() {
        SearchJob job = savedJob();
        profileRepository.save(profile("Grace", "https://linkedin.com/in/grace", "Harvard", job));
        profileRepository.save(profile("Henry", "https://linkedin.com/in/henry", "Yale", job));

        Page<AlumniProfile> results = profileRepository.findByFilters(null, null, null, PageRequest.of(0, 10));

        assertThat(results.getTotalElements()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("findByFilters filters by passout year")
    void findByFilters_passoutYear() {
        SearchJob job = savedJob();
        AlumniProfile p1 = profile("Iris", "https://linkedin.com/in/iris", "MIT", job);
        p1.setPassoutYear(2015);
        AlumniProfile p2 = profile("Jack", "https://linkedin.com/in/jack", "MIT", job);
        p2.setPassoutYear(2018);
        profileRepository.save(p1);
        profileRepository.save(p2);

        Page<AlumniProfile> results = profileRepository.findByFilters(null, null, 2015, PageRequest.of(0, 10));

        assertThat(results.getContent())
                .extracting(AlumniProfile::getName)
                .contains("Iris")
                .doesNotContain("Jack");
    }
}
