package com.taskmanagement.util;

import com.taskmanagement.domain.task.TaskDependency;
import com.taskmanagement.domain.task.TaskDependencyRepository;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Service for detecting circular dependencies in task dependency graphs using Depth-First Search (DFS).
 */
@Component
public class CircularDependencyDetector {

    private final TaskDependencyRepository dependencyRepository;

    public CircularDependencyDetector(TaskDependencyRepository dependencyRepository) {
        this.dependencyRepository = dependencyRepository;
    }

    /**
     * Check if adding a dependency from predecessorId to taskId would create a cycle.
     * Returns true if a cycle would be created, false otherwise.
     *
     * @param taskId The task that depends on the predecessor
     * @param predecessorId The task that must finish first
     * @return true if adding this dependency would create a cycle
     */
    public boolean wouldCreateCycle(Long taskId, Long predecessorId) {
        if (taskId == null || predecessorId == null) {
            throw new IllegalArgumentException("Task IDs cannot be null");
        }

        // Self-dependency is a cycle
        if (taskId.equals(predecessorId)) {
            return true;
        }

        // Check if there's already a path from taskId to predecessorId
        // If yes, adding edge (predecessorId -> taskId) would create a cycle
        Set<Long> visited = new HashSet<>();
        return hasPath(taskId, predecessorId, visited);
    }

    /**
     * Check if there exists a path from 'from' task to 'to' task in the dependency graph.
     * Uses Depth-First Search (DFS).
     */
    private boolean hasPath(Long from, Long to, Set<Long> visited) {
        // If we've reached the target, path exists
        if (from.equals(to)) {
            return true;
        }

        // If we've already visited this node, no need to explore again
        if (visited.contains(from)) {
            return false;
        }

        // Mark this node as visited
        visited.add(from);

        // Get all dependencies where 'from' is the predecessor
        // This means 'from' must finish before these tasks can start
        List<TaskDependency> outgoingDependencies = dependencyRepository.findByPredecessorTaskId(from);

        // Explore all successor tasks (tasks that depend on 'from')
        for (TaskDependency dependency : outgoingDependencies) {
            Long successorId = dependency.getTask().getId();
            if (hasPath(successorId, to, visited)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Detect all tasks involved in circular dependencies for a given project.
     * This is useful for validation during import operations.
     *
     * @param projectId The project to check
     * @return Set of task IDs that are part of circular dependencies
     */
    public Set<Long> detectAllCyclesInProject(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID cannot be null");
        }

        Set<Long> tasksInCycles = new HashSet<>();
        List<TaskDependency> allDependencies = dependencyRepository.findByProjectId(projectId);

        // Get all unique task IDs in the project
        Set<Long> allTaskIds = new HashSet<>();
        for (TaskDependency dep : allDependencies) {
            allTaskIds.add(dep.getTask().getId());
            allTaskIds.add(dep.getPredecessorTask().getId());
        }

        // For each task, check if it's part of a cycle
        for (Long taskId : allTaskIds) {
            if (isPartOfCycle(taskId, new HashSet<>(), new HashSet<>())) {
                tasksInCycles.add(taskId);
            }
        }

        return tasksInCycles;
    }

    /**
     * Check if a specific task is part of a cycle using DFS with recursion stack tracking.
     */
    private boolean isPartOfCycle(Long taskId, Set<Long> visited, Set<Long> recursionStack) {
        // Add to visited set
        visited.add(taskId);
        recursionStack.add(taskId);

        // Get all tasks that depend on this task
        List<TaskDependency> outgoingDependencies = dependencyRepository.findByPredecessorTaskId(taskId);

        for (TaskDependency dependency : outgoingDependencies) {
            Long successorId = dependency.getTask().getId();

            // If successor is not visited, recursively check
            if (!visited.contains(successorId)) {
                if (isPartOfCycle(successorId, visited, recursionStack)) {
                    return true;
                }
            }
            // If successor is in recursion stack, we found a cycle
            else if (recursionStack.contains(successorId)) {
                return true;
            }
        }

        // Remove from recursion stack before returning
        recursionStack.remove(taskId);
        return false;
    }

    /**
     * Get the dependency chain that would form a cycle if the given dependency were added.
     * Useful for error reporting.
     *
     * @param taskId The task that would depend on the predecessor
     * @param predecessorId The task that must finish first
     * @return List of task IDs in the cycle chain, or empty list if no cycle
     */
    public List<Long> getCycleChain(Long taskId, Long predecessorId) {
        if (taskId == null || predecessorId == null) {
            throw new IllegalArgumentException("Task IDs cannot be null");
        }

        if (taskId.equals(predecessorId)) {
            return List.of(taskId);
        }

        List<Long> path = new ArrayList<>();
        Set<Long> visited = new HashSet<>();

        if (findCyclePath(taskId, predecessorId, visited, path)) {
            path.add(0, predecessorId);
            path.add(taskId);
            return path;
        }

        return Collections.emptyList();
    }

    private boolean findCyclePath(Long from, Long to, Set<Long> visited, List<Long> path) {
        if (from.equals(to)) {
            return true;
        }

        if (visited.contains(from)) {
            return false;
        }

        visited.add(from);
        path.add(from);

        List<TaskDependency> outgoingDependencies = dependencyRepository.findByPredecessorTaskId(from);

        for (TaskDependency dependency : outgoingDependencies) {
            Long successorId = dependency.getTask().getId();
            if (findCyclePath(successorId, to, visited, path)) {
                return true;
            }
        }

        path.remove(path.size() - 1);
        return false;
    }
}
