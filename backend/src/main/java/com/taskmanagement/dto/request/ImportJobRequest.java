package com.taskmanagement.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for import job creation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportJobRequest {
    private Long projectId;
    private Boolean dryRun;
}
