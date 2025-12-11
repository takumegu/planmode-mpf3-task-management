package com.taskmanagement.domain.importjob;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {

    List<ImportJob> findByStatusOrderByExecutedAtDesc(String status);

    List<ImportJob> findAllByOrderByExecutedAtDesc();
}
