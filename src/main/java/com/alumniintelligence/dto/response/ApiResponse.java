package com.alumniintelligence.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Standardized API response envelope used by every endpoint.
 *
 * <pre>
 * {
 *   "status": "success" | "error",
 *   "message": "...",
 *   "data": {...},
 *   "errors": [...],
 *   "timestamp": "2024-..."
 * }
 * </pre>
 *
 * @param <T> the type of the {@code data} field
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized API response envelope")
public record ApiResponse<T>(

        @Schema(description = "success or error")
        String status,

        @Schema(description = "Human-readable message")
        String message,

        @Schema(description = "Response payload")
        T data,

        @Schema(description = "Validation or error details")
        List<String> errors,

        @Schema(description = "Response timestamp (ISO-8601)")
        Instant timestamp
) {

    // ----------------------------------------------------------------
    // Static factory methods
    // ----------------------------------------------------------------

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", null, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("success", message, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("error", message, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        return new ApiResponse<>("error", message, null, errors, Instant.now());
    }
}
