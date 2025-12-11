package com.taskmanagement.domain.importjob;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "import_job")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Source type is required")
    @Column(name = "source_type", nullable = false, length = 16)
    private String sourceType;

    @NotBlank(message = "Status is required")
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "executed_at", nullable = false)
    private OffsetDateTime executedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> summary;

    @Column(name = "error_report_path", columnDefinition = "TEXT")
    private String errorReportPath;

    @PrePersist
    protected void onCreate() {
        if (executedAt == null) {
            executedAt = OffsetDateTime.now();
        }
        if (summary == null) {
            summary = Map.of();
        }
    }
}
