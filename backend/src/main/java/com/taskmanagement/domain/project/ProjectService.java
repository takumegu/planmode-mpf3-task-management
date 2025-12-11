package com.taskmanagement.domain.project;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * Get all projects
     */
    @Transactional(readOnly = true)
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    /**
     * Get projects by status
     */
    @Transactional(readOnly = true)
    public List<Project> getProjectsByStatus(String status) {
        return projectRepository.findByStatus(status);
    }

    /**
     * Get a project by ID
     */
    @Transactional(readOnly = true)
    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));
    }

    /**
     * Create a new project
     */
    public Project createProject(Project project) {
        if (project.getId() != null) {
            throw new IllegalArgumentException("New project should not have an ID");
        }
        validateProject(project);
        return projectRepository.save(project);
    }

    /**
     * Update an existing project
     */
    public Project updateProject(Long id, Project projectUpdates) {
        Project existingProject = getProjectById(id);

        // Update fields
        if (projectUpdates.getName() != null) {
            existingProject.setName(projectUpdates.getName());
        }
        if (projectUpdates.getStartDate() != null) {
            existingProject.setStartDate(projectUpdates.getStartDate());
        }
        if (projectUpdates.getEndDate() != null) {
            existingProject.setEndDate(projectUpdates.getEndDate());
        }
        if (projectUpdates.getStatus() != null) {
            existingProject.setStatus(projectUpdates.getStatus());
        }

        validateProject(existingProject);
        return projectRepository.save(existingProject);
    }

    /**
     * Delete a project
     */
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new EntityNotFoundException("Project not found with id: " + id);
        }
        projectRepository.deleteById(id);
    }

    /**
     * Search projects by name
     */
    @Transactional(readOnly = true)
    public List<Project> searchProjectsByName(String name) {
        return projectRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Validate project constraints
     */
    private void validateProject(Project project) {
        if (project.getName() == null || project.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Project name is required");
        }

        if (project.getStartDate() != null && project.getEndDate() != null &&
            project.getStartDate().isAfter(project.getEndDate())) {
            throw new IllegalArgumentException("Project start date must not be after end date");
        }

        String status = project.getStatus();
        if (status != null && !status.equals("active") && !status.equals("archived")) {
            throw new IllegalArgumentException("Invalid project status: " + status);
        }
    }
}
