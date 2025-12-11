package com.taskmanagement.domain.task;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "task_dependency",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"task_id", "predecessor_task_id"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Task is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @NotNull(message = "Predecessor task is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predecessor_task_id", nullable = false)
    private Task predecessorTask;

    @Column(name = "type", nullable = false, length = 8)
    private String type = "FS";

    @PrePersist
    @PreUpdate
    protected void validate() {
        if (task != null && predecessorTask != null &&
            task.getId() != null && task.getId().equals(predecessorTask.getId())) {
            throw new IllegalArgumentException("Task cannot depend on itself");
        }
        if (type == null) {
            type = "FS";
        }
    }
}
