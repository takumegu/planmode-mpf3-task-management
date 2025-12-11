package com.taskmanagement.dto.request;

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
public class UpdateProjectRequest {

    @Size(max = 200, message = "Project name must not exceed 200 characters")
    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    private String status;
}
