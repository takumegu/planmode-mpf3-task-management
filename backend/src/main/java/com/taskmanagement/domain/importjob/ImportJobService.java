package com.taskmanagement.domain.importjob;

import com.taskmanagement.domain.importjob.parser.CsvParser;
import com.taskmanagement.domain.importjob.parser.ExcelParser;
import com.taskmanagement.domain.importjob.parser.ImportValidator;
import com.taskmanagement.domain.importjob.parser.ParsedTaskData;
import com.taskmanagement.domain.project.Project;
import com.taskmanagement.domain.project.ProjectRepository;
import com.taskmanagement.domain.task.Task;
import com.taskmanagement.domain.task.TaskDependency;
import com.taskmanagement.domain.task.TaskDependencyRepository;
import com.taskmanagement.domain.task.TaskRepository;
import com.taskmanagement.dto.response.ImportJobResponse;
import com.taskmanagement.dto.response.ValidationError;
import com.taskmanagement.util.ErrorCsvGenerator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Service for handling CSV/Excel import operations
 * Supports dry-run validation and two-phase import (tasks then dependencies)
 */
@Service
public class ImportJobService {

    private final ImportJobRepository importJobRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final CsvParser csvParser;
    private final ExcelParser excelParser;
    private final ImportValidator validator;
    private final ErrorCsvGenerator errorCsvGenerator;

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    };

    public ImportJobService(ImportJobRepository importJobRepository,
                            ProjectRepository projectRepository,
                            TaskRepository taskRepository,
                            TaskDependencyRepository taskDependencyRepository,
                            CsvParser csvParser,
                            ExcelParser excelParser,
                            ImportValidator validator,
                            ErrorCsvGenerator errorCsvGenerator) {
        this.importJobRepository = importJobRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.taskDependencyRepository = taskDependencyRepository;
        this.csvParser = csvParser;
        this.excelParser = excelParser;
        this.validator = validator;
        this.errorCsvGenerator = errorCsvGenerator;
    }

    /**
     * Execute import job with optional dry-run mode
     *
     * @param file      Uploaded CSV or Excel file
     * @param projectId Target project ID
     * @param dryRun    If true, only validate without committing
     * @return Import job response with results
     */
    @Transactional
    public ImportJobResponse executeImport(MultipartFile file, Long projectId, boolean dryRun)
            throws IOException {

        // Validate project exists
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));

        // Determine file type and parse
        String filename = file.getOriginalFilename();
        String sourceType;
        List<ParsedTaskData> parsedData;

        if (filename != null && filename.toLowerCase().endsWith(".csv")) {
            sourceType = "CSV";
            parsedData = csvParser.parse(file);
        } else if (filename != null && (filename.toLowerCase().endsWith(".xlsx") ||
                filename.toLowerCase().endsWith(".xls"))) {
            sourceType = "Excel";
            parsedData = excelParser.parse(file);
        } else {
            throw new IllegalArgumentException("Unsupported file type. Only CSV and Excel (.xlsx) files are supported");
        }

        // Validate parsed data
        List<ValidationError> validationErrors = validator.validate(parsedData, projectId);

        // Create import job record
        ImportJob importJob = new ImportJob();
        importJob.setSourceType(sourceType);
        importJob.setExecutedAt(OffsetDateTime.now());

        ImportJobResponse.ImportSummary summary = ImportJobResponse.ImportSummary.builder()
                .totalRows(parsedData.size())
                .successfulRows(0)
                .failedRows(validationErrors.size())
                .tasksCreated(0)
                .tasksUpdated(0)
                .dependenciesCreated(0)
                .build();

        // If dry-run or validation failed, return results without committing
        if (dryRun || !validationErrors.isEmpty()) {
            importJob.setStatus(dryRun ? "DRY_RUN" : "FAILED");
            importJob.setSummary(summaryToMap(summary));

            // Generate error CSV if there are errors
            if (!validationErrors.isEmpty()) {
                try {
                    String errorCsvPath = errorCsvGenerator.generateErrorCsv(validationErrors, importJob.getId());
                    importJob.setErrorReportPath(errorCsvPath);
                } catch (IOException e) {
                    // Log error but don't fail the import job
                    System.err.println("Failed to generate error CSV: " + e.getMessage());
                }
            }

            importJob = importJobRepository.save(importJob);

            return ImportJobResponse.builder()
                    .id(importJob.getId())
                    .sourceType(importJob.getSourceType())
                    .status(importJob.getStatus())
                    .executedAt(importJob.getExecutedAt().toLocalDateTime())
                    .summary(summary)
                    .errors(validationErrors)
                    .build();
        }

        // Execute actual import (two-phase: tasks first, then dependencies)
        ImportResult result = executeActualImport(parsedData, project);

        summary.setSuccessfulRows(parsedData.size() - result.getFailedRows().size());
        summary.setFailedRows(result.getFailedRows().size());
        summary.setTasksCreated(result.getTasksCreated());
        summary.setTasksUpdated(result.getTasksUpdated());
        summary.setDependenciesCreated(result.getDependenciesCreated());

        importJob.setStatus(result.getFailedRows().isEmpty() ? "SUCCESS" : "PARTIAL");
        importJob.setSummary(summaryToMap(summary));

        // Generate error CSV if there are errors
        if (!result.getErrors().isEmpty()) {
            try {
                String errorCsvPath = errorCsvGenerator.generateErrorCsv(result.getErrors(), importJob.getId());
                importJob.setErrorReportPath(errorCsvPath);
            } catch (IOException e) {
                // Log error but don't fail the import job
                System.err.println("Failed to generate error CSV: " + e.getMessage());
            }
        }

        importJob = importJobRepository.save(importJob);

        return ImportJobResponse.builder()
                .id(importJob.getId())
                .sourceType(importJob.getSourceType())
                .status(importJob.getStatus())
                .executedAt(importJob.getExecutedAt().toLocalDateTime())
                .summary(summary)
                .errors(result.getErrors())
                .build();
    }

    /**
     * Execute the actual import with upsert logic
     */
    private ImportResult executeActualImport(List<ParsedTaskData> parsedData, Project project) {
        ImportResult result = new ImportResult();

        // Phase 1: Import/update tasks
        Map<String, Task> taskCodeToTaskMap = new HashMap<>();

        for (ParsedTaskData data : parsedData) {
            try {
                Task task = upsertTask(data, project, result);
                if (data.getTaskCode() != null) {
                    taskCodeToTaskMap.put(data.getTaskCode(), task);
                }
            } catch (Exception e) {
                result.getFailedRows().add(data.getLineNumber());
                result.getErrors().add(ValidationError.builder()
                        .lineNumber(data.getLineNumber())
                        .field("task")
                        .errorCode("IMPORT_ERROR")
                        .errorMessage("Failed to import task: " + e.getMessage())
                        .build());
            }
        }

        // Phase 2: Import dependencies
        for (ParsedTaskData data : parsedData) {
            if (data.getPredecessorTaskCodes() != null && data.getTaskCode() != null) {
                Task task = taskCodeToTaskMap.get(data.getTaskCode());
                if (task == null) {
                    // Task failed to import in phase 1, skip dependencies
                    continue;
                }

                String[] predecessors = data.getPredecessorTaskCodes().split(",");
                String dependencyType = data.getDependencyType() != null ?
                        data.getDependencyType().toUpperCase() : "FS";

                for (String predecessorCode : predecessors) {
                    String trimmed = predecessorCode.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }

                    try {
                        Task predecessorTask = findTaskByCode(trimmed, project.getId(), taskCodeToTaskMap);
                        if (predecessorTask != null) {
                            createDependencyIfNotExists(task, predecessorTask, dependencyType);
                            result.incrementDependenciesCreated();
                        }
                    } catch (Exception e) {
                        result.getErrors().add(ValidationError.builder()
                                .lineNumber(data.getLineNumber())
                                .field("predecessor_task_codes")
                                .value(trimmed)
                                .errorCode("DEPENDENCY_ERROR")
                                .errorMessage("Failed to create dependency: " + e.getMessage())
                                .build());
                    }
                }
            }
        }

        return result;
    }

    /**
     * Upsert task: create if new, update if exists (matched by project_id + task_code)
     */
    private Task upsertTask(ParsedTaskData data, Project project, ImportResult result) {
        Task task;
        boolean isUpdate = false;

        // Try to find existing task by task_code
        if (data.getTaskCode() != null) {
            task = taskRepository.findByProjectIdAndTaskCode(project.getId(), data.getTaskCode())
                    .orElse(new Task());
            isUpdate = task.getId() != null;
        } else {
            task = new Task();
        }

        // Set/update fields
        task.setProject(project);
        if (data.getTaskCode() != null) {
            task.setTaskCode(data.getTaskCode());
        }
        task.setName(data.getName());

        if (data.getAssignee() != null) {
            task.setAssignee(data.getAssignee());
        }

        task.setStartDate(parseDate(data.getStartDate()));
        task.setEndDate(parseDate(data.getEndDate()));

        if (data.getProgress() != null) {
            task.setProgress(Short.parseShort(data.getProgress()));
        } else {
            task.setProgress(isUpdate ? task.getProgress() : (short) 0);
        }

        if (data.getStatus() != null) {
            task.setStatus(data.getStatus().toLowerCase());
        } else {
            task.setStatus(isUpdate ? task.getStatus() : "planned");
        }

        if (data.getIsMilestone() != null) {
            task.setIsMilestone(parseBoolean(data.getIsMilestone()));
        } else {
            task.setIsMilestone(isUpdate ? task.getIsMilestone() : false);
        }

        if (data.getNotes() != null) {
            task.setNotes(data.getNotes());
        }

        // Handle parent task
        if (data.getParentTaskCode() != null) {
            Task parentTask = taskRepository.findByProjectIdAndTaskCode(project.getId(),
                    data.getParentTaskCode()).orElse(null);
            task.setParentTask(parentTask);
        }

        task = taskRepository.save(task);

        if (isUpdate) {
            result.incrementTasksUpdated();
        } else {
            result.incrementTasksCreated();
        }

        return task;
    }

    /**
     * Find task by code, checking both newly imported tasks and database
     */
    private Task findTaskByCode(String taskCode, Long projectId, Map<String, Task> newTaskMap) {
        // Check newly imported tasks first
        if (newTaskMap.containsKey(taskCode)) {
            return newTaskMap.get(taskCode);
        }

        // Check database
        return taskRepository.findByProjectIdAndTaskCode(projectId, taskCode).orElse(null);
    }

    /**
     * Create dependency if it doesn't already exist
     */
    private void createDependencyIfNotExists(Task task, Task predecessorTask, String type) {
        // Check if dependency already exists
        boolean exists = taskDependencyRepository
                .findByTaskIdAndPredecessorTaskId(task.getId(), predecessorTask.getId())
                .isPresent();

        if (!exists) {
            TaskDependency dependency = new TaskDependency();
            dependency.setTask(task);
            dependency.setPredecessorTask(predecessorTask);
            dependency.setType(type);
            taskDependencyRepository.save(dependency);
        }
    }

    /**
     * Parse date string with multiple format support
     */
    private LocalDate parseDate(String dateString) {
        if (dateString == null) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateString, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next formatter
            }
        }

        throw new IllegalArgumentException("Invalid date format: " + dateString);
    }

    /**
     * Parse boolean value from string
     */
    private boolean parseBoolean(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase();
        return lower.equals("true") || lower.equals("1") || lower.equals("yes");
    }

    /**
     * Convert ImportSummary to Map for JSON storage
     */
    private Map<String, Object> summaryToMap(ImportJobResponse.ImportSummary summary) {
        Map<String, Object> map = new HashMap<>();
        map.put("totalRows", summary.getTotalRows());
        map.put("successfulRows", summary.getSuccessfulRows());
        map.put("failedRows", summary.getFailedRows());
        map.put("tasksCreated", summary.getTasksCreated());
        map.put("tasksUpdated", summary.getTasksUpdated());
        map.put("dependenciesCreated", summary.getDependenciesCreated());
        return map;
    }

    /**
     * Get import job by ID
     */
    public ImportJobResponse getImportJob(Long id) {
        ImportJob job = importJobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Import job not found with id: " + id));

        ImportJobResponse.ImportSummary summary = mapToSummary(job.getSummary());

        return ImportJobResponse.builder()
                .id(job.getId())
                .sourceType(job.getSourceType())
                .status(job.getStatus())
                .executedAt(job.getExecutedAt().toLocalDateTime())
                .summary(summary)
                .build();
    }

    /**
     * Convert Map to ImportSummary
     */
    private ImportJobResponse.ImportSummary mapToSummary(Map<String, Object> map) {
        return ImportJobResponse.ImportSummary.builder()
                .totalRows(getIntValue(map, "totalRows"))
                .successfulRows(getIntValue(map, "successfulRows"))
                .failedRows(getIntValue(map, "failedRows"))
                .tasksCreated(getIntValue(map, "tasksCreated"))
                .tasksUpdated(getIntValue(map, "tasksUpdated"))
                .dependenciesCreated(getIntValue(map, "dependenciesCreated"))
                .build();
    }

    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    /**
     * Helper class to track import results
     */
    private static class ImportResult {
        private final List<Integer> failedRows = new ArrayList<>();
        private final List<ValidationError> errors = new ArrayList<>();
        private int tasksCreated = 0;
        private int tasksUpdated = 0;
        private int dependenciesCreated = 0;

        public List<Integer> getFailedRows() {
            return failedRows;
        }

        public List<ValidationError> getErrors() {
            return errors;
        }

        public int getTasksCreated() {
            return tasksCreated;
        }

        public void incrementTasksCreated() {
            this.tasksCreated++;
        }

        public int getTasksUpdated() {
            return tasksUpdated;
        }

        public void incrementTasksUpdated() {
            this.tasksUpdated++;
        }

        public int getDependenciesCreated() {
            return dependenciesCreated;
        }

        public void incrementDependenciesCreated() {
            this.dependenciesCreated++;
        }
    }
}
