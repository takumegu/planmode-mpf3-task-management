package com.taskmanagement.domain.task;

import com.taskmanagement.domain.project.Project;
import com.taskmanagement.domain.project.ProjectRepository;
import com.taskmanagement.util.CircularDependencyDetector;
import com.taskmanagement.util.WorkingDayCalculator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskDependencyRepository dependencyRepository;
    private final ProjectRepository projectRepository;
    private final WorkingDayCalculator workingDayCalculator;
    private final CircularDependencyDetector circularDependencyDetector;

    public TaskService(
        TaskRepository taskRepository,
        TaskDependencyRepository dependencyRepository,
        ProjectRepository projectRepository,
        WorkingDayCalculator workingDayCalculator,
        CircularDependencyDetector circularDependencyDetector
    ) {
        this.taskRepository = taskRepository;
        this.dependencyRepository = dependencyRepository;
        this.projectRepository = projectRepository;
        this.workingDayCalculator = workingDayCalculator;
        this.circularDependencyDetector = circularDependencyDetector;
    }

    /**
     * Get all tasks for a project
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    /**
     * Get tasks by project and date range
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByProjectAndDateRange(Long projectId, LocalDate startDate, LocalDate endDate) {
        return taskRepository.findByProjectIdAndDateRange(projectId, startDate, endDate);
    }

    /**
     * Get tasks by project and status
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByProjectAndStatus(Long projectId, String status) {
        return taskRepository.findByProjectIdAndStatus(projectId, status);
    }

    /**
     * Get a task by ID
     */
    @Transactional(readOnly = true)
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + id));
    }

    /**
     * Create a new task
     */
    public Task createTask(Long projectId, Task task) {
        if (task.getId() != null) {
            throw new IllegalArgumentException("New task should not have an ID");
        }

        // Set project
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));
        task.setProject(project);

        // Adjust dates to working days if needed
        task.setStartDate(workingDayCalculator.adjustToWorkingDay(task.getStartDate()));
        task.setEndDate(workingDayCalculator.adjustToWorkingDay(task.getEndDate()));

        validateTask(task);
        return taskRepository.save(task);
    }

    /**
     * Update an existing task
     */
    public Task updateTask(Long id, Task taskUpdates) {
        Task existingTask = getTaskById(id);

        // Update fields
        if (taskUpdates.getName() != null) {
            existingTask.setName(taskUpdates.getName());
        }
        if (taskUpdates.getAssignee() != null) {
            existingTask.setAssignee(taskUpdates.getAssignee());
        }
        if (taskUpdates.getStartDate() != null) {
            LocalDate adjustedStart = workingDayCalculator.adjustToWorkingDay(taskUpdates.getStartDate());
            existingTask.setStartDate(adjustedStart);
        }
        if (taskUpdates.getEndDate() != null) {
            LocalDate adjustedEnd = workingDayCalculator.adjustToWorkingDay(taskUpdates.getEndDate());
            existingTask.setEndDate(adjustedEnd);
        }
        if (taskUpdates.getProgress() != null) {
            existingTask.setProgress(taskUpdates.getProgress());
        }
        if (taskUpdates.getStatus() != null) {
            existingTask.setStatus(taskUpdates.getStatus());
        }
        if (taskUpdates.getParentTask() != null) {
            existingTask.setParentTask(taskUpdates.getParentTask());
        }
        if (taskUpdates.getIsMilestone() != null) {
            existingTask.setIsMilestone(taskUpdates.getIsMilestone());
        }
        if (taskUpdates.getNotes() != null) {
            existingTask.setNotes(taskUpdates.getNotes());
        }

        validateTask(existingTask);
        return taskRepository.save(existingTask);
    }

    /**
     * Delete a task
     */
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new EntityNotFoundException("Task not found with id: " + id);
        }
        taskRepository.deleteById(id);
    }

    /**
     * Create a dependency between tasks
     */
    public TaskDependency createDependency(Long taskId, Long predecessorTaskId, String type) {
        Task task = getTaskById(taskId);
        Task predecessorTask = getTaskById(predecessorTaskId);

        // Check for circular dependency
        if (circularDependencyDetector.wouldCreateCycle(taskId, predecessorTaskId)) {
            List<Long> cycle = circularDependencyDetector.getCycleChain(taskId, predecessorTaskId);
            throw new IllegalArgumentException(
                "Creating this dependency would create a circular dependency. Cycle: " + cycle
            );
        }

        // Check if dependency already exists
        if (dependencyRepository.findByTaskIdAndPredecessorTaskId(taskId, predecessorTaskId).isPresent()) {
            throw new IllegalArgumentException("Dependency already exists");
        }

        TaskDependency dependency = TaskDependency.builder()
            .task(task)
            .predecessorTask(predecessorTask)
            .type(type != null ? type : "FS")
            .build();

        return dependencyRepository.save(dependency);
    }

    /**
     * Delete a dependency
     */
    public void deleteDependency(Long dependencyId) {
        if (!dependencyRepository.existsById(dependencyId)) {
            throw new EntityNotFoundException("Dependency not found with id: " + dependencyId);
        }
        dependencyRepository.deleteById(dependencyId);
    }

    /**
     * Get all dependencies for a task
     */
    @Transactional(readOnly = true)
    public List<TaskDependency> getDependenciesByTask(Long taskId) {
        return dependencyRepository.findByTaskId(taskId);
    }

    /**
     * Get all tasks that depend on a given task
     */
    @Transactional(readOnly = true)
    public List<TaskDependency> getDependenciesByPredecessor(Long predecessorTaskId) {
        return dependencyRepository.findByPredecessorTaskId(predecessorTaskId);
    }

    /**
     * Validate task constraints
     */
    private void validateTask(Task task) {
        if (task.getName() == null || task.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Task name is required");
        }

        if (task.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }

        if (task.getEndDate() == null) {
            throw new IllegalArgumentException("End date is required");
        }

        if (task.getStartDate().isAfter(task.getEndDate())) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }

        if (task.getProgress() != null && (task.getProgress() < 0 || task.getProgress() > 100)) {
            throw new IllegalArgumentException("Progress must be between 0 and 100");
        }

        String status = task.getStatus();
        if (status != null) {
            List<String> validStatuses = List.of("planned", "in_progress", "done", "blocked", "on_hold");
            if (!validStatuses.contains(status)) {
                throw new IllegalArgumentException("Invalid task status: " + status);
            }
        }

        // Validate parent task is not self
        if (task.getParentTask() != null && task.getId() != null &&
            task.getParentTask().getId().equals(task.getId())) {
            throw new IllegalArgumentException("Task cannot be its own parent");
        }
    }
}
