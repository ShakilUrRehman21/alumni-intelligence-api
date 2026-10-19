package com.alumniintelligence.mapper;

import com.alumniintelligence.dto.response.AlumniProfileResponse;
import com.alumniintelligence.dto.response.SearchJobResponse;
import com.alumniintelligence.entity.AlumniProfile;
import com.alumniintelligence.entity.SearchJob;
import org.springframework.stereotype.Component;

/**
 * Manual mapper between JPA entities and response DTOs.
 *
 * <p>Kept as a plain Spring {@code @Component} to avoid a MapStruct
 * annotation-processor dependency, keeping the build simpler while still
 * isolating mapping logic from both the service and controller layers.
 */
@Component
public class AlumniMapper {

    public AlumniProfileResponse toProfileResponse(AlumniProfile profile) {
        return new AlumniProfileResponse(
                profile.getId(),
                profile.getName(),
                profile.getCurrentRole(),
                profile.getUniversity(),
                profile.getLocation(),
                profile.getLinkedinHeadline(),
                profile.getLinkedinProfileUrl(),
                profile.getPassoutYear(),
                profile.getSearchJob() != null ? profile.getSearchJob().getId() : null,
                profile.getCreatedAt()
        );
    }

    public SearchJobResponse toJobResponse(SearchJob job) {
        return new SearchJobResponse(
                job.getId(),
                job.getUniversity(),
                job.getDesignation(),
                job.getPassoutYear(),
                job.getStatus(),
                job.getResultCount(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getCompletedAt()
        );
    }
}
