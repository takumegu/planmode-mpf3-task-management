package com.taskmanagement.domain.task;

import com.taskmanagement.domain.project.Project;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "task",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "task_code"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Size(max = 64, message = "Task code must not exceed 64 characters")
    @Column(name = "task_code", length = 64)
    private String taskCode;

    @NotBlank(message = "Task name is required")
    @Size(max = 255, message = "Task name must not exceed 255 characters")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Size(max = 120, message = "Assignee must not exceed 120 characters")
    @Column(name = "assignee", length = 120)
    private String assignee;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Min(value = 0, message = "Progress must be between 0 and 100")
    @Max(value = 100, message = "Progress must be between 0 and 100")
    @Column(name = "progress", nullable = false)
    private Short progress = 0;

    @NotBlank(message = "Status is required")
    @Column(name = "status", nullable = false, length = 32)
    private String status = "planned";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private Task parentTask;

    @Column(name = "is_milestone", nullable = false)
    private Boolean isMilestone = false;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (progress == null) {
            progress = 0;
        }
        if (status == null) {
            status = "planned";
        }
        if (isMilestone == null) {
            isMilestone = false;
        }
        validateDates();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
        validateDates();
    }

    private void validateDates() {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }
    }

    // Prevent self-referencing parent task
    public void setParentTask(Task parentTask) {
        if (parentTask != null && parentTask.getId() != null && parentTask.getId().equals(this.id)) {
            throw new IllegalArgumentException("Task cannot be its own parent");
        }
        this.parentTask = parentTask;
    }
}
