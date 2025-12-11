package com.taskmanagement.domain.importjob.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single row of parsed task data from CSV or Excel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedTaskData {
    private Integer lineNumber;
    private String taskCode;
    private String name;
    private String assignee;
    private String startDate;
    private String endDate;
    private String progress;
    private String status;
    private String parentTaskCode;
    private String isMilestone;
    private String predecessorTaskCodes; // Comma-separated list
    private String dependencyType;
    private String notes;
}
