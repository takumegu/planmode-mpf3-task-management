package com.taskmanagement.domain.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    List<Task> findByProjectIdAndStatus(Long projectId, String status);

    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId " +
           "AND t.startDate <= :endDate AND t.endDate >= :startDate")
    List<Task> findByProjectIdAndDateRange(
        @Param("projectId") Long projectId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    Optional<Task> findByProjectIdAndTaskCode(Long projectId, String taskCode);

    List<Task> findByParentTaskId(Long parentTaskId);
}
