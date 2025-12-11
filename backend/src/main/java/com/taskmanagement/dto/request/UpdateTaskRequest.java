package com.taskmanagement.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskRequest {

    @Size(max = 255, message = "Task name must not exceed 255 characters")
    private String name;

    @Size(max = 120, message = "Assignee must not exceed 120 characters")
    private String assignee;

    private LocalDate startDate;

    private LocalDate endDate;

    @Min(value = 0, message = "Progress must be between 0 and 100")
    @Max(value = 100, message = "Progress must be between 0 and 100")
    private Short progress;

    private String status;

    private Long parentTaskId;

    private Boolean isMilestone;

    private String notes;
}
