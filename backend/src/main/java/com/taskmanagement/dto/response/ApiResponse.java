package com.taskmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Generic API response wrapper
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private T data;
    private Meta meta;
    private List<ApiError> errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        private String requestId;
        private OffsetDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiError {
        private String code;
        private String message;
        private String field;
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .data(data)
            .meta(Meta.builder()
                .requestId(UUID.randomUUID().toString())
                .timestamp(OffsetDateTime.now())
                .build())
            .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
            .errors(List.of(ApiError.builder()
                .code(code)
                .message(message)
                .build()))
            .meta(Meta.builder()
                .requestId(UUID.randomUUID().toString())
                .timestamp(OffsetDateTime.now())
                .build())
            .build();
    }

    public static <T> ApiResponse<T> error(List<ApiError> errors) {
        return ApiResponse.<T>builder()
            .errors(errors)
            .meta(Meta.builder()
                .requestId(UUID.randomUUID().toString())
                .timestamp(OffsetDateTime.now())
                .build())
            .build();
    }
}
