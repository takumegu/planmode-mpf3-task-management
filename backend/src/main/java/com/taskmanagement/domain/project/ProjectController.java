package com.taskmanagement.domain.project;

import com.taskmanagement.dto.request.CreateProjectRequest;
import com.taskmanagement.dto.request.UpdateProjectRequest;
import com.taskmanagement.dto.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * GET /api/projects - List all projects
     */
    @GetMapping
    public ApiResponse<List<Project>> getAllProjects(
        @RequestParam(required = false) String status
    ) {
        List<Project> projects;
        if (status != null) {
            projects = projectService.getProjectsByStatus(status);
        } else {
            projects = projectService.getAllProjects();
        }
        return ApiResponse.success(projects);
    }

    /**
     * GET /api/projects/{id} - Get project by ID
     */
    @GetMapping("/{id}")
    public ApiResponse<Project> getProjectById(@PathVariable Long id) {
        Project project = projectService.getProjectById(id);
        return ApiResponse.success(project);
    }

    /**
     * POST /api/projects - Create new project
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Project> createProject(
        @Valid @RequestBody CreateProjectRequest request
    ) {
        Project project = Project.builder()
            .name(request.getName())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .status(request.getStatus() != null ? request.getStatus() : "active")
            .build();

        Project createdProject = projectService.createProject(project);
        return ApiResponse.success(createdProject);
    }

    /**
     * PATCH /api/projects/{id} - Update project
     */
    @PatchMapping("/{id}")
    public ApiResponse<Project> updateProject(
        @PathVariable Long id,
        @Valid @RequestBody UpdateProjectRequest request
    ) {
        Project projectUpdates = Project.builder()
            .name(request.getName())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .status(request.getStatus())
            .build();

        Project updatedProject = projectService.updateProject(id, projectUpdates);
        return ApiResponse.success(updatedProject);
    }

    /**
     * DELETE /api/projects/{id} - Delete project
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
    }

    /**
     * GET /api/projects/search - Search projects by name
     */
    @GetMapping("/search")
    public ApiResponse<List<Project>> searchProjects(
        @RequestParam String name
    ) {
        List<Project> projects = projectService.searchProjectsByName(name);
        return ApiResponse.success(projects);
    }
}
