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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AlumniService}.
 *
 * <p>All external dependencies are mocked with Mockito. No Spring context is loaded.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AlumniServiceTest {

    @Mock private AlumniProfileRepository profileRepository;
    @Mock private SearchJobRepository     jobRepository;
    @Mock private PhantomBusterClient     phantomBusterClient;
    @Mock private AlumniMapper            mapper;

    @InjectMocks
    private AlumniService alumniService;

    private SearchJob pendingJob;
    private AlumniSearchRequest validRequest;

    @BeforeEach
    void setUp() {
        pendingJob = SearchJob.builder()
                .id(1L)
                .university("University of XYZ")
                .designation("Software Engineer")
                .passoutYear(2020)
                .build();

        validRequest = new AlumniSearchRequest(
                "University of XYZ", "Software Engineer", 2020);
    }

    // ----------------------------------------------------------------
    // initiateSearch
    // ----------------------------------------------------------------

    @Test
    @DisplayName("initiateSearch saves PENDING job and returns job response")
    void initiateSearch_shouldSavePendingJobAndReturnResponse() {
        when(jobRepository.save(any(SearchJob.class))).thenReturn(pendingJob);
        // @Async runs synchronously in unit tests (no Spring context), so
        // executeSearch will be called inline — stub findById so it doesn't throw.
        when(jobRepository.findById(1L)).thenReturn(Optional.of(pendingJob));
        when(phantomBusterClient.searchAlumni(any(), any(), any())).thenReturn(List.of());
        when(profileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SearchJobResponse mockResponse = new SearchJobResponse(
                1L, "University of XYZ", "Software Engineer", 2020,
                JobStatus.PENDING, 0, null, null, null);
        when(mapper.toJobResponse(pendingJob)).thenReturn(mockResponse);

        SearchJobResponse result = alumniService.initiateSearch(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(JobStatus.PENDING);

        ArgumentCaptor<SearchJob> captor = ArgumentCaptor.forClass(SearchJob.class);
        verify(jobRepository, atLeastOnce()).save(captor.capture());
        SearchJob saved = captor.getAllValues().get(0);
        assertThat(saved.getUniversity()).isEqualTo("University of XYZ");
        assertThat(saved.getDesignation()).isEqualTo("Software Engineer");
        assertThat(saved.getPassoutYear()).isEqualTo(2020);
    }

    @Test
    @DisplayName("initiateSearch strips whitespace from university and designation")
    void initiateSearch_shouldStripWhitespace() {
        AlumniSearchRequest requestWithSpaces = new AlumniSearchRequest(
                "  MIT  ", "  Data Scientist  ", null);
        SearchJob savedJob = SearchJob.builder().id(2L).university("MIT").designation("Data Scientist").build();
        when(jobRepository.save(any())).thenReturn(savedJob);
        when(jobRepository.findById(2L)).thenReturn(Optional.of(savedJob));
        when(phantomBusterClient.searchAlumni(any(), any(), any())).thenReturn(List.of());
        when(profileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toJobResponse(any())).thenReturn(mock(SearchJobResponse.class));

        alumniService.initiateSearch(requestWithSpaces);

        ArgumentCaptor<SearchJob> captor = ArgumentCaptor.forClass(SearchJob.class);
        verify(jobRepository, atLeastOnce()).save(captor.capture());
        SearchJob firstSave = captor.getAllValues().get(0);
        assertThat(firstSave.getUniversity()).isEqualTo("MIT");
        assertThat(firstSave.getDesignation()).isEqualTo("Data Scientist");
    }

    // ----------------------------------------------------------------
    // executeSearch
    // ----------------------------------------------------------------

    @Test
    @DisplayName("executeSearch persists profiles and marks job COMPLETED")
    void executeSearch_shouldPersistProfilesAndCompleteJob() {
        SearchJob job = SearchJob.builder()
                .id(1L).university("MIT").designation("Engineer").passoutYear(null).build();
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenReturn(job);

        List<PhantomBusterProfile> profiles = List.of(
                new PhantomBusterProfile("Alice", "Engineer", "Corp", "MIT", "NY",
                        "Headline", "https://linkedin.com/in/alice", 2020));
        when(phantomBusterClient.searchAlumni(eq("MIT"), eq("Engineer"), isNull()))
                .thenReturn(profiles);
        when(profileRepository.findByLinkedinProfileUrl(any())).thenReturn(Optional.empty());
        when(profileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        alumniService.executeSearch(1L);

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.getResultCount()).isEqualTo(1);
        verify(profileRepository).save(any(AlumniProfile.class));
    }

    @Test
    @DisplayName("executeSearch deduplicates by LinkedIn URL — updates existing profile")
    void executeSearch_shouldUpdateExistingProfileOnDuplicate() {
        SearchJob job = SearchJob.builder()
                .id(1L).university("MIT").designation("Engineer").build();
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenReturn(job);

        PhantomBusterProfile rawProfile = new PhantomBusterProfile(
                "Bob Updated", "Senior Engineer", "BigCorp", "MIT",
                "London", "Updated headline", "https://linkedin.com/in/bob", 2019);
        when(phantomBusterClient.searchAlumni(any(), any(), any()))
                .thenReturn(List.of(rawProfile));

        AlumniProfile existing = AlumniProfile.builder().id(99L)
                .name("Bob Old").linkedinProfileUrl("https://linkedin.com/in/bob").build();
        when(profileRepository.findByLinkedinProfileUrl("https://linkedin.com/in/bob"))
                .thenReturn(Optional.of(existing));
        when(profileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        alumniService.executeSearch(1L);

        ArgumentCaptor<AlumniProfile> captor = ArgumentCaptor.forClass(AlumniProfile.class);
        verify(profileRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(99L); // same existing entity
        assertThat(captor.getValue().getName()).isEqualTo("Bob Updated");
    }

    @Test
    @DisplayName("executeSearch marks job FAILED when PhantomBuster throws")
    void executeSearch_shouldMarkJobFailedOnPhantomBusterException() {
        SearchJob job = SearchJob.builder()
                .id(1L).university("MIT").designation("Engineer").build();
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenReturn(job);
        when(phantomBusterClient.searchAlumni(any(), any(), any()))
                .thenThrow(new PhantomBusterException("API key invalid"));

        alumniService.executeSearch(1L);

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).contains("API key invalid");
        verify(profileRepository, never()).save(any());
    }

    @Test
    @DisplayName("executeSearch throws ResourceNotFoundException when job ID not found")
    void executeSearch_shouldThrowWhenJobNotFound() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alumniService.executeSearch(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ----------------------------------------------------------------
    // Read operations
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getProfileById returns response when profile exists")
    void getProfileById_shouldReturnProfile() {
        AlumniProfile profile = AlumniProfile.builder().id(1L).name("Alice").build();
        AlumniProfileResponse response = new AlumniProfileResponse(
                1L, "Alice", null, "MIT", null, null, null, null, null, null);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(mapper.toProfileResponse(profile)).thenReturn(response);

        AlumniProfileResponse result = alumniService.getProfileById(1L);
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("getProfileById throws ResourceNotFoundException for unknown ID")
    void getProfileById_shouldThrowForUnknownId() {
        when(profileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alumniService.getProfileById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("getAllProfiles returns paginated result")
    void getAllProfiles_shouldReturnPage() {
        AlumniProfile profile = AlumniProfile.builder().id(1L).name("Alice").build();
        Page<AlumniProfile> page = new PageImpl<>(List.of(profile));
        when(profileRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);
        AlumniProfileResponse resp = new AlumniProfileResponse(1L, "Alice", null, "MIT", null, null, null, null, null, null);
        when(mapper.toProfileResponse(profile)).thenReturn(resp);

        Page<AlumniProfileResponse> result = alumniService.getAllProfiles(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Alice");
    }
}
