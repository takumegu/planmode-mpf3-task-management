package com.taskmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDependencyRequest {

    @NotNull(message = "Predecessor task ID is required")
    private Long predecessorTaskId;

    private String type; // Defaults to "FS" if not provided
}
