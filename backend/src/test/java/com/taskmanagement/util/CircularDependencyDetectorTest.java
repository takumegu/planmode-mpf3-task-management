package com.taskmanagement.util;

import com.taskmanagement.domain.task.Task;
import com.taskmanagement.domain.task.TaskDependency;
import com.taskmanagement.domain.task.TaskDependencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CircularDependencyDetectorTest {

    @Mock
    private TaskDependencyRepository dependencyRepository;

    private CircularDependencyDetector detector;

    @BeforeEach
    void setUp() {
        detector = new CircularDependencyDetector(dependencyRepository);
    }

    @Test
    void testSelfDependencyIsDetected() {
        // Task cannot depend on itself
        assertTrue(detector.wouldCreateCycle(1L, 1L));
    }

    @Test
    void testDirectCycleIsDetected() {
        // Setup: Task 1 -> Task 2 (existing)
        // Testing: Task 2 -> Task 1 (would create cycle)
        Task task1 = createTask(1L);
        Task task2 = createTask(2L);

        TaskDependency dep = createDependency(2L, 1L, task2, task1);
        when(dependencyRepository.findByPredecessorTaskId(2L)).thenReturn(List.of(dep));
        when(dependencyRepository.findByPredecessorTaskId(1L)).thenReturn(List.of());

        // Adding dependency 1 -> 2 would create a cycle
        assertTrue(detector.wouldCreateCycle(1L, 2L));
    }

    @Test
    void testIndirectCycleIsDetected() {
        // Setup: Task 1 -> Task 2 -> Task 3 (existing)
        // Testing: Task 3 -> Task 1 (would create cycle)
        Task task1 = createTask(1L);
        Task task2 = createTask(2L);
        Task task3 = createTask(3L);

        TaskDependency dep1to2 = createDependency(2L, 1L, task2, task1);
        TaskDependency dep2to3 = createDependency(3L, 2L, task3, task2);

        when(dependencyRepository.findByPredecessorTaskId(3L)).thenReturn(List.of());
        when(dependencyRepository.findByPredecessorTaskId(2L)).thenReturn(List.of(dep2to3));
        when(dependencyRepository.findByPredecessorTaskId(1L)).thenReturn(List.of(dep1to2));

        // Adding dependency 1 -> 3 would create a cycle: 1 -> 2 -> 3 -> 1
        assertTrue(detector.wouldCreateCycle(1L, 3L));
    }

    @Test
    void testNoCycleForLinearDependency() {
        // Setup: Task 1 -> Task 2 (existing)
        // Testing: Task 2 -> Task 3 (no cycle)
        Task task1 = createTask(1L);
        Task task2 = createTask(2L);

        TaskDependency dep = createDependency(2L, 1L, task2, task1);
        when(dependencyRepository.findByPredecessorTaskId(1L)).thenReturn(List.of(dep));
        when(dependencyRepository.findByPredecessorTaskId(2L)).thenReturn(List.of());
        when(dependencyRepository.findByPredecessorTaskId(3L)).thenReturn(List.of());

        // Adding dependency 3 -> 2 creates: 1 -> 2 -> 3 (no cycle)
        assertFalse(detector.wouldCreateCycle(3L, 2L));
    }

    @Test
    void testNoCycleForParallelDependencies() {
        // Setup: Task 1 -> Task 2, Task 1 -> Task 3 (existing)
        // Testing: Task 2 -> Task 4 (no cycle)
        Task task1 = createTask(1L);
        Task task2 = createTask(2L);
        Task task3 = createTask(3L);

        TaskDependency dep1to2 = createDependency(2L, 1L, task2, task1);
        TaskDependency dep1to3 = createDependency(3L, 1L, task3, task1);

        when(dependencyRepository.findByPredecessorTaskId(1L)).thenReturn(List.of(dep1to2, dep1to3));
        when(dependencyRepository.findByPredecessorTaskId(2L)).thenReturn(List.of());
        when(dependencyRepository.findByPredecessorTaskId(3L)).thenReturn(List.of());
        when(dependencyRepository.findByPredecessorTaskId(4L)).thenReturn(List.of());

        // Adding dependency 4 -> 2 creates: 1 -> 2 -> 4 (no cycle)
        assertFalse(detector.wouldCreateCycle(4L, 2L));
    }

    @Test
    void testDetectAllCyclesInProject() {
        // Setup: Task 1 -> Task 2 -> Task 3 -> Task 1 (cycle)
        //        Task 4 -> Task 5 (no cycle)
        Task task1 = createTask(1L);
        Task task2 = createTask(2L);
        Task task3 = createTask(3L);
        Task task4 = createTask(4L);
        Task task5 = createTask(5L);

        TaskDependency dep1to2 = createDependency(2L, 1L, task2, task1);
        TaskDependency dep2to3 = createDependency(3L, 2L, task3, task2);
        TaskDependency dep3to1 = createDependency(1L, 3L, task1, task3);
        TaskDependency dep4to5 = createDependency(5L, 4L, task5, task4);

        List<TaskDependency> allDeps = List.of(dep1to2, dep2to3, dep3to1, dep4to5);
        when(dependencyRepository.findByProjectId(1L)).thenReturn(allDeps);

        when(dependencyRepository.findByPredecessorTaskId(1L)).thenReturn(List.of(dep1to2));
        when(dependencyRepository.findByPredecessorTaskId(2L)).thenReturn(List.of(dep2to3));
        when(dependencyRepository.findByPredecessorTaskId(3L)).thenReturn(List.of(dep3to1));
        when(dependencyRepository.findByPredecessorTaskId(4L)).thenReturn(List.of(dep4to5));
        when(dependencyRepository.findByPredecessorTaskId(5L)).thenReturn(List.of());

        Set<Long> tasksInCycles = detector.detectAllCyclesInProject(1L);

        // Tasks 1, 2, 3 are in a cycle; tasks 4, 5 are not
        assertTrue(tasksInCycles.contains(1L));
        assertTrue(tasksInCycles.contains(2L));
        assertTrue(tasksInCycles.contains(3L));
        assertFalse(tasksInCycles.contains(4L));
        assertFalse(tasksInCycles.contains(5L));
    }

    @Test
    void testGetCycleChain() {
        // Setup: Task 1 -> Task 2 -> Task 3 (existing)
        // Testing: Get cycle chain for 3 -> 1
        Task task1 = createTask(1L);
        Task task2 = createTask(2L);
        Task task3 = createTask(3L);

        TaskDependency dep1to2 = createDependency(2L, 1L, task2, task1);
        TaskDependency dep2to3 = createDependency(3L, 2L, task3, task2);

        when(dependencyRepository.findByPredecessorTaskId(3L)).thenReturn(List.of());
        when(dependencyRepository.findByPredecessorTaskId(2L)).thenReturn(List.of(dep2to3));
        when(dependencyRepository.findByPredecessorTaskId(1L)).thenReturn(List.of(dep1to2));

        List<Long> cycleChain = detector.getCycleChain(1L, 3L);

        // Expected chain: 3 -> 1 -> 2 -> 3 -> 1
        assertFalse(cycleChain.isEmpty());
        assertTrue(cycleChain.contains(1L));
        assertTrue(cycleChain.contains(2L));
        assertTrue(cycleChain.contains(3L));
    }

    @Test
    void testGetCycleChainForSelfDependency() {
        List<Long> cycleChain = detector.getCycleChain(1L, 1L);

        // Self-dependency cycle chain should just contain the task itself
        assertEquals(1, cycleChain.size());
        assertEquals(1L, cycleChain.get(0));
    }

    @Test
    void testGetCycleChainForNoCycle() {
        // Setup: Task 1 -> Task 2 (existing)
        // Testing: Get cycle chain for 3 -> 1 (no cycle)
        Task task1 = createTask(1L);
        Task task2 = createTask(2L);

        TaskDependency dep = createDependency(2L, 1L, task2, task1);
        when(dependencyRepository.findByPredecessorTaskId(1L)).thenReturn(List.of(dep));
        when(dependencyRepository.findByPredecessorTaskId(2L)).thenReturn(List.of());
        when(dependencyRepository.findByPredecessorTaskId(3L)).thenReturn(List.of());

        List<Long> cycleChain = detector.getCycleChain(3L, 1L);

        // No cycle, so chain should be empty
        assertTrue(cycleChain.isEmpty());
    }

    @Test
    void testNullParametersThrowException() {
        assertThrows(IllegalArgumentException.class, () -> detector.wouldCreateCycle(null, 1L));
        assertThrows(IllegalArgumentException.class, () -> detector.wouldCreateCycle(1L, null));
        assertThrows(IllegalArgumentException.class, () -> detector.detectAllCyclesInProject(null));
        assertThrows(IllegalArgumentException.class, () -> detector.getCycleChain(null, 1L));
        assertThrows(IllegalArgumentException.class, () -> detector.getCycleChain(1L, null));
    }

    // Helper methods
    private Task createTask(Long id) {
        Task task = new Task();
        task.setId(id);
        return task;
    }

    private TaskDependency createDependency(Long taskId, Long predecessorId, Task task, Task predecessor) {
        TaskDependency dep = new TaskDependency();
        dep.setTask(task);
        dep.setPredecessorTask(predecessor);
        return dep;
    }
}
