package com.taskmanagement.domain.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {

    List<TaskDependency> findByTaskId(Long taskId);

    List<TaskDependency> findByPredecessorTaskId(Long predecessorTaskId);

    @Query("SELECT td FROM TaskDependency td WHERE td.task.project.id = :projectId")
    List<TaskDependency> findByProjectId(@Param("projectId") Long projectId);

    Optional<TaskDependency> findByTaskIdAndPredecessorTaskId(Long taskId, Long predecessorTaskId);

    void deleteByTaskIdAndPredecessorTaskId(Long taskId, Long predecessorTaskId);
}
