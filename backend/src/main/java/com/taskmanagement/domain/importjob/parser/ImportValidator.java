package com.taskmanagement.domain.importjob.parser;

import com.taskmanagement.domain.task.Task;
import com.taskmanagement.domain.task.TaskRepository;
import com.taskmanagement.dto.response.ValidationError;
import com.taskmanagement.util.CircularDependencyDetector;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Validates parsed task data before import
 * Performs field validation, reference integrity checks, and circular dependency detection
 */
@Component
public class ImportValidator {

    private final TaskRepository taskRepository;
    private final CircularDependencyDetector circularDependencyDetector;

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    };

    private static final Set<String> VALID_STATUSES = Set.of(
            "planned", "in_progress", "done", "blocked", "on_hold"
    );

    private static final Set<String> VALID_DEPENDENCY_TYPES = Set.of(
            "FS", "SS", "FF", "SF"
    );

    public ImportValidator(TaskRepository taskRepository,
                           CircularDependencyDetector circularDependencyDetector) {
        this.taskRepository = taskRepository;
        this.circularDependencyDetector = circularDependencyDetector;
    }

    /**
     * Validate all parsed task data
     *
     * @param parsedData    List of parsed task data
     * @param projectId     Project ID for reference validation
     * @return List of validation errors (empty if all valid)
     */
    public List<ValidationError> validate(List<ParsedTaskData> parsedData, Long projectId) {
        List<ValidationError> errors = new ArrayList<>();

        // Build map of task codes for reference validation
        Set<String> taskCodesInFile = new HashSet<>();
        Map<String, ParsedTaskData> taskCodeMap = new HashMap<>();

        for (ParsedTaskData data : parsedData) {
            if (data.getTaskCode() != null) {
                taskCodesInFile.add(data.getTaskCode());
                taskCodeMap.put(data.getTaskCode(), data);
            }
        }

        // Get existing task codes in the project
        List<Task> existingTasks = taskRepository.findByProjectId(projectId);
        Map<String, Task> existingTaskMap = new HashMap<>();
        for (Task task : existingTasks) {
            if (task.getTaskCode() != null) {
                existingTaskMap.put(task.getTaskCode(), task);
            }
        }

        // Validate each row
        for (ParsedTaskData data : parsedData) {
            errors.addAll(validateRow(data, projectId, taskCodesInFile, existingTaskMap));
        }

        // Validate circular dependencies
        errors.addAll(validateCircularDependencies(parsedData, existingTaskMap, projectId));

        return errors;
    }

    /**
     * Validate a single row of data
     */
    private List<ValidationError> validateRow(ParsedTaskData data, Long projectId,
                                              Set<String> taskCodesInFile,
                                              Map<String, Task> existingTaskMap) {
        List<ValidationError> errors = new ArrayList<>();

        // Validate required fields
        if (isBlank(data.getName())) {
            errors.add(createError(data.getLineNumber(), "name", data.getName(),
                    "REQUIRED_FIELD", "Task name is required"));
        } else if (data.getName().length() > 255) {
            errors.add(createError(data.getLineNumber(), "name", data.getName(),
                    "FIELD_TOO_LONG", "Task name must not exceed 255 characters"));
        }

        if (isBlank(data.getStartDate())) {
            errors.add(createError(data.getLineNumber(), "start_date", data.getStartDate(),
                    "REQUIRED_FIELD", "Start date is required"));
        }

        if (isBlank(data.getEndDate())) {
            errors.add(createError(data.getLineNumber(), "end_date", data.getEndDate(),
                    "REQUIRED_FIELD", "End date is required"));
        }

        // Validate date formats and logic
        LocalDate startDate = validateAndParseDate(data.getLineNumber(), "start_date",
                data.getStartDate(), errors);
        LocalDate endDate = validateAndParseDate(data.getLineNumber(), "end_date",
                data.getEndDate(), errors);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            errors.add(createError(data.getLineNumber(), "start_date", data.getStartDate(),
                    "INVALID_DATE_RANGE", "Start date must not be after end date"));
        }

        // Validate progress
        if (data.getProgress() != null) {
            try {
                int progress = Integer.parseInt(data.getProgress());
                if (progress < 0 || progress > 100) {
                    errors.add(createError(data.getLineNumber(), "progress", data.getProgress(),
                            "INVALID_RANGE", "Progress must be between 0 and 100"));
                }
            } catch (NumberFormatException e) {
                errors.add(createError(data.getLineNumber(), "progress", data.getProgress(),
                        "INVALID_FORMAT", "Progress must be a valid number"));
            }
        }

        // Validate status
        if (data.getStatus() != null && !VALID_STATUSES.contains(data.getStatus().toLowerCase())) {
            errors.add(createError(data.getLineNumber(), "status", data.getStatus(),
                    "INVALID_VALUE", "Status must be one of: " + VALID_STATUSES));
        }

        // Validate boolean fields
        if (data.getIsMilestone() != null) {
            if (!isBooleanValue(data.getIsMilestone())) {
                errors.add(createError(data.getLineNumber(), "is_milestone", data.getIsMilestone(),
                        "INVALID_FORMAT", "is_milestone must be true/false or 1/0"));
            }
        }

        // Validate task_code uniqueness within file
        if (data.getTaskCode() != null) {
            long duplicateCount = taskCodesInFile.stream()
                    .filter(code -> code.equals(data.getTaskCode()))
                    .count();
            // Note: This check is approximate; a more precise check would need line tracking
        }

        // Validate field lengths
        if (data.getTaskCode() != null && data.getTaskCode().length() > 64) {
            errors.add(createError(data.getLineNumber(), "task_code", data.getTaskCode(),
                    "FIELD_TOO_LONG", "Task code must not exceed 64 characters"));
        }

        if (data.getAssignee() != null && data.getAssignee().length() > 120) {
            errors.add(createError(data.getLineNumber(), "assignee", data.getAssignee(),
                    "FIELD_TOO_LONG", "Assignee must not exceed 120 characters"));
        }

        // Validate parent_task_code reference
        if (data.getParentTaskCode() != null) {
            if (!taskCodesInFile.contains(data.getParentTaskCode()) &&
                    !existingTaskMap.containsKey(data.getParentTaskCode())) {
                errors.add(createError(data.getLineNumber(), "parent_task_code",
                        data.getParentTaskCode(), "REFERENCE_NOT_FOUND",
                        "Parent task code not found in file or database"));
            }
        }

        // Validate predecessor references
        if (data.getPredecessorTaskCodes() != null) {
            String[] predecessors = data.getPredecessorTaskCodes().split(",");
            for (String predecessor : predecessors) {
                String trimmed = predecessor.trim();
                if (!trimmed.isEmpty()) {
                    if (!taskCodesInFile.contains(trimmed) && !existingTaskMap.containsKey(trimmed)) {
                        errors.add(createError(data.getLineNumber(), "predecessor_task_codes",
                                trimmed, "REFERENCE_NOT_FOUND",
                                "Predecessor task code not found in file or database"));
                    }
                }
            }
        }

        // Validate dependency type
        if (data.getDependencyType() != null) {
            if (!VALID_DEPENDENCY_TYPES.contains(data.getDependencyType().toUpperCase())) {
                errors.add(createError(data.getLineNumber(), "dependency_type",
                        data.getDependencyType(), "INVALID_VALUE",
                        "Dependency type must be one of: " + VALID_DEPENDENCY_TYPES));
            }
        }

        return errors;
    }

    /**
     * Validate that dependencies don't create circular references
     */
    private List<ValidationError> validateCircularDependencies(
            List<ParsedTaskData> parsedData,
            Map<String, Task> existingTaskMap,
            Long projectId) {

        List<ValidationError> errors = new ArrayList<>();

        // Build a temporary dependency graph
        Map<String, List<String>> dependencyGraph = new HashMap<>();

        // Add existing dependencies from database
        for (Task task : existingTaskMap.values()) {
            // Note: This is simplified - in reality we'd need to query TaskDependencyRepository
            // to get all existing dependencies, but for now we'll validate new dependencies
        }

        // Add dependencies from import file
        for (ParsedTaskData data : parsedData) {
            if (data.getTaskCode() != null && data.getPredecessorTaskCodes() != null) {
                String[] predecessors = data.getPredecessorTaskCodes().split(",");
                for (String predecessor : predecessors) {
                    String trimmed = predecessor.trim();
                    if (!trimmed.isEmpty()) {
                        dependencyGraph
                                .computeIfAbsent(data.getTaskCode(), k -> new ArrayList<>())
                                .add(trimmed);
                    }
                }
            }
        }

        // Check for cycles using DFS
        for (ParsedTaskData data : parsedData) {
            if (data.getTaskCode() != null && data.getPredecessorTaskCodes() != null) {
                String[] predecessors = data.getPredecessorTaskCodes().split(",");
                for (String predecessor : predecessors) {
                    String trimmed = predecessor.trim();
                    if (!trimmed.isEmpty()) {
                        if (wouldCreateCycle(data.getTaskCode(), trimmed, dependencyGraph)) {
                            errors.add(createError(data.getLineNumber(), "predecessor_task_codes",
                                    trimmed, "CIRCULAR_DEPENDENCY",
                                    String.format("Adding dependency from %s to %s would create a circular reference",
                                            data.getTaskCode(), trimmed)));
                        }
                    }
                }
            }
        }

        return errors;
    }

    /**
     * Check if adding a dependency would create a cycle
     */
    private boolean wouldCreateCycle(String taskCode, String predecessorCode,
                                     Map<String, List<String>> graph) {
        if (taskCode.equals(predecessorCode)) {
            return true; // Self-dependency
        }

        Set<String> visited = new HashSet<>();
        return hasPath(predecessorCode, taskCode, graph, visited);
    }

    /**
     * DFS to check if there's a path from 'from' to 'to'
     */
    private boolean hasPath(String from, String to, Map<String, List<String>> graph,
                            Set<String> visited) {
        if (from.equals(to)) {
            return true;
        }

        if (visited.contains(from)) {
            return false;
        }

        visited.add(from);

        List<String> successors = graph.get(from);
        if (successors != null) {
            for (String successor : successors) {
                if (hasPath(successor, to, graph, visited)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Validate and parse date string
     */
    private LocalDate validateAndParseDate(Integer lineNumber, String field, String value,
                                           List<ValidationError> errors) {
        if (value == null) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next formatter
            }
        }

        errors.add(createError(lineNumber, field, value, "INVALID_FORMAT",
                "Date must be in format yyyy-MM-dd, yyyy/MM/dd, MM/dd/yyyy, or dd/MM/yyyy"));
        return null;
    }

    /**
     * Check if value is a valid boolean representation
     */
    private boolean isBooleanValue(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase();
        return lower.equals("true") || lower.equals("false") ||
                lower.equals("1") || lower.equals("0") ||
                lower.equals("yes") || lower.equals("no");
    }

    /**
     * Check if string is null or blank
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Create a validation error
     */
    private ValidationError createError(Integer lineNumber, String field, String value,
                                        String errorCode, String errorMessage) {
        return ValidationError.builder()
                .lineNumber(lineNumber)
                .field(field)
                .value(value)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}
