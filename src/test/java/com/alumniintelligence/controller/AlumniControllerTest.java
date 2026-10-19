package com.alumniintelligence.controller;

import com.alumniintelligence.dto.response.AlumniProfileResponse;
import com.alumniintelligence.dto.response.SearchJobResponse;
import com.alumniintelligence.entity.JobStatus;
import com.alumniintelligence.exception.GlobalExceptionHandler;
import com.alumniintelligence.exception.ResourceNotFoundException;
import com.alumniintelligence.service.AlumniService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * {@link WebMvcTest} slice tests for {@link AlumniController}.
 *
 * <p>Verifies HTTP routing, request validation, status codes, and response shape.
 * Service layer is mocked.
 */
@WebMvcTest(AlumniController.class)
@Import(GlobalExceptionHandler.class)
class AlumniControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  AlumniService alumniService;

    // ----------------------------------------------------------------
    // POST /api/alumni/search
    // ----------------------------------------------------------------

    @Test
    @DisplayName("POST /api/alumni/search — valid request returns 202 Accepted")
    void postSearch_validRequest_returns202() throws Exception {
        SearchJobResponse job = new SearchJobResponse(
                1L, "University of XYZ", "Software Engineer", 2020,
                JobStatus.PENDING, 0, null, OffsetDateTime.now(), null);
        when(alumniService.initiateSearch(any())).thenReturn(job);

        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "university", "University of XYZ",
                                "designation", "Software Engineer",
                                "passoutYear", 2020
                        ))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/alumni/search — missing university returns 400")
    void postSearch_missingUniversity_returns400() throws Exception {
        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "designation", "Software Engineer"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /api/alumni/search — blank university returns 400")
    void postSearch_blankUniversity_returns400() throws Exception {
        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "university", "   ",
                                "designation", "Engineer"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("POST /api/alumni/search — invalid passoutYear returns 400")
    void postSearch_invalidPassoutYear_returns400() throws Exception {
        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "university", "MIT",
                                "designation", "Engineer",
                                "passoutYear", 1850
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("POST /api/alumni/search — missing designation returns 400")
    void postSearch_missingDesignation_returns400() throws Exception {
        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "university", "MIT"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("POST /api/alumni/search — malformed JSON returns 400")
    void postSearch_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    // ----------------------------------------------------------------
    // GET /api/alumni/all
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /api/alumni/all — returns 200 with profile page")
    void getAllProfiles_returns200() throws Exception {
        AlumniProfileResponse profile = new AlumniProfileResponse(
                1L, "John Doe", "Software Engineer", "MIT",
                "New York", "Headline", "https://linkedin.com/in/johndoe",
                2020, 1L, OffsetDateTime.now());
        when(alumniService.getAllProfiles(any())).thenReturn(new PageImpl<>(List.of(profile)));

        mockMvc.perform(get("/api/alumni/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content[0].name").value("John Doe"))
                .andExpect(jsonPath("$.data.content[0].currentRole").value("Software Engineer"));
    }

    // ----------------------------------------------------------------
    // GET /api/alumni/{id}
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /api/alumni/{id} — existing ID returns 200")
    void getProfileById_existingId_returns200() throws Exception {
        AlumniProfileResponse profile = new AlumniProfileResponse(
                42L, "Jane Smith", "Data Scientist", "Stanford",
                "SF", null, null, 2019, null, null);
        when(alumniService.getProfileById(42L)).thenReturn(profile);

        mockMvc.perform(get("/api/alumni/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.name").value("Jane Smith"));
    }

    @Test
    @DisplayName("GET /api/alumni/{id} — unknown ID returns 404")
    void getProfileById_unknownId_returns404() throws Exception {
        when(alumniService.getProfileById(999L))
                .thenThrow(new ResourceNotFoundException("Alumni profile not found: 999"));

        mockMvc.perform(get("/api/alumni/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Alumni profile not found: 999"));
    }

    // ----------------------------------------------------------------
    // GET /api/alumni/jobs/{jobId}
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /api/alumni/jobs/{jobId} — returns job status")
    void getJobStatus_returnsJob() throws Exception {
        SearchJobResponse job = new SearchJobResponse(
                5L, "Harvard", "Lawyer", null,
                JobStatus.COMPLETED, 3, null, OffsetDateTime.now(), OffsetDateTime.now());
        when(alumniService.getJobById(5L)).thenReturn(job);

        mockMvc.perform(get("/api/alumni/jobs/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.resultCount").value(3));
    }

    @Test
    @DisplayName("GET /api/alumni/jobs/{jobId} — unknown job returns 404")
    void getJobStatus_unknownJob_returns404() throws Exception {
        when(alumniService.getJobById(999L))
                .thenThrow(new ResourceNotFoundException("Search job not found: 999"));

        mockMvc.perform(get("/api/alumni/jobs/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));
    }
}
