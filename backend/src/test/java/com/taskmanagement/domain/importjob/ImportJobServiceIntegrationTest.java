package com.taskmanagement.domain.importjob;

import com.taskmanagement.domain.project.Project;
import com.taskmanagement.domain.project.ProjectRepository;
import com.taskmanagement.domain.task.Task;
import com.taskmanagement.domain.task.TaskDependency;
import com.taskmanagement.domain.task.TaskDependencyRepository;
import com.taskmanagement.domain.task.TaskRepository;
import com.taskmanagement.dto.response.ImportJobResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ImportJobServiceIntegrationTest {

    @Autowired
    private ImportJobService importJobService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskDependencyRepository taskDependencyRepository;

    @Autowired
    private ImportJobRepository importJobRepository;

    private Project testProject;

    @BeforeEach
    void setUp() {
        // Clean up
        taskDependencyRepository.deleteAll();
        taskRepository.deleteAll();
        importJobRepository.deleteAll();
        projectRepository.deleteAll();

        // Create test project
        testProject = Project.builder()
                .name("Test Project")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 12, 31))
                .status("active")
                .build();
        testProject = projectRepository.save(testProject);
    }

    @Test
    void testImportCsvSuccessfully() throws IOException {
        String csvContent = """
                task_code,name,assignee,start_date,end_date,progress,status
                TASK-001,Planning,John Doe,2025-01-01,2025-01-15,0,planned
                TASK-002,Development,Jane Smith,2025-01-16,2025-02-28,0,planned
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tasks.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        ImportJobResponse response = importJobService.executeImport(file, testProject.getId(), false);

        assertEquals("CSV", response.getSourceType());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(2, response.getSummary().getTotalRows());
        assertEquals(2, response.getSummary().getSuccessfulRows());
        assertEquals(0, response.getSummary().getFailedRows());
        assertEquals(2, response.getSummary().getTasksCreated());

        // Verify tasks in database
        List<Task> tasks = taskRepository.findByProjectId(testProject.getId());
        assertEquals(2, tasks.size());
    }

    @Test
    void testImportWithDependencies() throws IOException {
        String csvContent = """
                task_code,name,start_date,end_date,predecessor_task_codes
                TASK-001,Task 1,2025-01-01,2025-01-10,
                TASK-002,Task 2,2025-01-11,2025-01-20,TASK-001
                TASK-003,Task 3,2025-01-21,2025-01-30,"TASK-001,TASK-002"
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tasks.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        ImportJobResponse response = importJobService.executeImport(file, testProject.getId(), false);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(3, response.getSummary().getTasksCreated());
        assertEquals(3, response.getSummary().getDependenciesCreated());

        // Verify dependencies
        List<Task> tasks = taskRepository.findByProjectId(testProject.getId());
        Task task2 = tasks.stream().filter(t -> "TASK-002".equals(t.getTaskCode())).findFirst().orElse(null);
        assertNotNull(task2);

        List<TaskDependency> task2Deps = taskDependencyRepository.findByTaskId(task2.getId());
        assertEquals(1, task2Deps.size());
    }

    @Test
    void testDryRunMode() throws IOException {
        String csvContent = """
                task_code,name,start_date,end_date
                TASK-001,Valid Task,2025-01-01,2025-01-10
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tasks.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        ImportJobResponse response = importJobService.executeImport(file, testProject.getId(), true);

        assertEquals("DRY_RUN", response.getStatus());
        assertEquals(1, response.getSummary().getTotalRows());

        // Verify no tasks were created
        List<Task> tasks = taskRepository.findByProjectId(testProject.getId());
        assertEquals(0, tasks.size());
    }

    @Test
    void testValidationErrors() throws IOException {
        String csvContent = """
                task_code,name,start_date,end_date
                TASK-001,,2025-01-01,2025-01-10
                TASK-002,Invalid Dates,2025-02-01,2025-01-10
                TASK-003,Invalid Progress,2025-01-01,2025-01-10
                """;

        csvContent = csvContent.replace("TASK-003,Invalid Progress", "TASK-003,Invalid Progress");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tasks.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        ImportJobResponse response = importJobService.executeImport(file, testProject.getId(), false);

        assertEquals("FAILED", response.getStatus());
        assertTrue(response.getErrors().size() > 0);
    }

    @Test
    void testCircularDependencyDetection() throws IOException {
        String csvContent = """
                task_code,name,start_date,end_date,predecessor_task_codes
                TASK-001,Task 1,2025-01-01,2025-01-10,TASK-003
                TASK-002,Task 2,2025-01-11,2025-01-20,TASK-001
                TASK-003,Task 3,2025-01-21,2025-01-30,TASK-002
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tasks.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        ImportJobResponse response = importJobService.executeImport(file, testProject.getId(), false);

        assertEquals("FAILED", response.getStatus());
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> "CIRCULAR_DEPENDENCY".equals(error.getErrorCode())));
    }

    @Test
    void testUpsertLogic() throws IOException {
        // First import
        String csvContent1 = """
                task_code,name,start_date,end_date,progress
                TASK-001,Initial Name,2025-01-01,2025-01-10,0
                """;

        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "tasks.csv",
                "text/csv",
                csvContent1.getBytes(StandardCharsets.UTF_8)
        );

        ImportJobResponse response1 = importJobService.executeImport(file1, testProject.getId(), false);
        assertEquals(1, response1.getSummary().getTasksCreated());

        // Second import (update)
        String csvContent2 = """
                task_code,name,start_date,end_date,progress
                TASK-001,Updated Name,2025-01-01,2025-01-15,50
                """;

        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "tasks.csv",
                "text/csv",
                csvContent2.getBytes(StandardCharsets.UTF_8)
        );

        ImportJobResponse response2 = importJobService.executeImport(file2, testProject.getId(), false);
        assertEquals(1, response2.getSummary().getTasksUpdated());
        assertEquals(0, response2.getSummary().getTasksCreated());

        // Verify update
        Task task = taskRepository.findByProjectIdAndTaskCode(testProject.getId(), "TASK-001").orElse(null);
        assertNotNull(task);
        assertEquals("Updated Name", task.getName());
        assertEquals((short) 50, task.getProgress());
        assertEquals(LocalDate.of(2025, 1, 15), task.getEndDate());
    }

    @Test
    void testInvalidFileType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tasks.txt",
                "text/plain",
                "invalid content".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(IllegalArgumentException.class, () -> {
            importJobService.executeImport(file, testProject.getId(), false);
        });
    }

    @Test
    void testEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tasks.csv",
                "text/csv",
                new byte[0]
        );

        assertThrows(IllegalArgumentException.class, () -> {
            importJobService.executeImport(file, testProject.getId(), false);
        });
    }
}
