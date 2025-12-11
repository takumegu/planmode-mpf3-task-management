package com.taskmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for import job status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportJobResponse {
    private Long id;
    private String sourceType;
    private String status;
    private LocalDateTime executedAt;
    private ImportSummary summary;
    private List<ValidationError> errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportSummary {
        private Integer totalRows;
        private Integer successfulRows;
        private Integer failedRows;
        private Integer tasksCreated;
        private Integer tasksUpdated;
        private Integer dependenciesCreated;
    }
}
