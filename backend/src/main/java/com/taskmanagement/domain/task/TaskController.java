package com.taskmanagement.domain.task;

import com.taskmanagement.dto.request.CreateDependencyRequest;
import com.taskmanagement.dto.request.CreateTaskRequest;
import com.taskmanagement.dto.request.UpdateTaskRequest;
import com.taskmanagement.dto.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * GET /api/projects/{projectId}/tasks - List tasks for a project
     */
    @GetMapping("/projects/{projectId}/tasks")
    public ApiResponse<List<Task>> getTasksByProject(
        @PathVariable Long projectId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) String status
    ) {
        List<Task> tasks;

        if (from != null && to != null) {
            tasks = taskService.getTasksByProjectAndDateRange(projectId, from, to);
        } else if (status != null) {
            tasks = taskService.getTasksByProjectAndStatus(projectId, status);
        } else {
            tasks = taskService.getTasksByProject(projectId);
        }

        return ApiResponse.success(tasks);
    }

    /**
     * POST /api/projects/{projectId}/tasks - Create task
     */
    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Task> createTask(
        @PathVariable Long projectId,
        @Valid @RequestBody CreateTaskRequest request
    ) {
        Task task = Task.builder()
            .taskCode(request.getTaskCode())
            .name(request.getName())
            .assignee(request.getAssignee())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .progress(request.getProgress() != null ? request.getProgress() : (short) 0)
            .status(request.getStatus() != null ? request.getStatus() : "planned")
            .isMilestone(request.getIsMilestone() != null ? request.getIsMilestone() : false)
            .notes(request.getNotes())
            .build();

        // Handle parent task if provided
        if (request.getParentTaskId() != null) {
            Task parentTask = taskService.getTaskById(request.getParentTaskId());
            task.setParentTask(parentTask);
        }

        Task createdTask = taskService.createTask(projectId, task);
        return ApiResponse.success(createdTask);
    }

    /**
     * GET /api/tasks/{id} - Get task by ID
     */
    @GetMapping("/tasks/{id}")
    public ApiResponse<Task> getTaskById(@PathVariable Long id) {
        Task task = taskService.getTaskById(id);
        return ApiResponse.success(task);
    }

    /**
     * PATCH /api/tasks/{id} - Update task
     */
    @PatchMapping("/tasks/{id}")
    public ApiResponse<Task> updateTask(
        @PathVariable Long id,
        @Valid @RequestBody UpdateTaskRequest request
    ) {
        Task taskUpdates = Task.builder()
            .name(request.getName())
            .assignee(request.getAssignee())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .progress(request.getProgress())
            .status(request.getStatus())
            .isMilestone(request.getIsMilestone())
            .notes(request.getNotes())
            .build();

        // Handle parent task if provided
        if (request.getParentTaskId() != null) {
            Task parentTask = taskService.getTaskById(request.getParentTaskId());
            taskUpdates.setParentTask(parentTask);
        }

        Task updatedTask = taskService.updateTask(id, taskUpdates);
        return ApiResponse.success(updatedTask);
    }

    /**
     * DELETE /api/tasks/{id} - Delete task
     */
    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
    }

    /**
     * POST /api/tasks/{taskId}/dependencies - Create dependency
     */
    @PostMapping("/tasks/{taskId}/dependencies")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TaskDependency> createDependency(
        @PathVariable Long taskId,
        @Valid @RequestBody CreateDependencyRequest request
    ) {
        TaskDependency dependency = taskService.createDependency(
            taskId,
            request.getPredecessorTaskId(),
            request.getType()
        );
        return ApiResponse.success(dependency);
    }

    /**
     * DELETE /api/tasks/{taskId}/dependencies/{dependencyId} - Delete dependency
     */
    @DeleteMapping("/tasks/{taskId}/dependencies/{dependencyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDependency(
        @PathVariable Long taskId,
        @PathVariable Long dependencyId
    ) {
        taskService.deleteDependency(dependencyId);
    }

    /**
     * GET /api/tasks/{taskId}/dependencies - Get task dependencies
     */
    @GetMapping("/tasks/{taskId}/dependencies")
    public ApiResponse<List<TaskDependency>> getTaskDependencies(
        @PathVariable Long taskId
    ) {
        List<TaskDependency> dependencies = taskService.getDependenciesByTask(taskId);
        return ApiResponse.success(dependencies);
    }
}
