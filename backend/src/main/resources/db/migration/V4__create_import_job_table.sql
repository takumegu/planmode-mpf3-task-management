-- Create import_job table for tracking CSV/Excel imports
CREATE TABLE import_job (
    id BIGSERIAL PRIMARY KEY,
    source_type VARCHAR(16) NOT NULL CHECK (source_type IN ('CSV', 'Excel')),
    status VARCHAR(16) NOT NULL
        CHECK (status IN ('PENDING', 'DRY_RUN', 'SUCCESS', 'PARTIAL', 'FAILED')),
    executed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    summary JSONB NOT NULL DEFAULT '{}',
    error_report_path TEXT
);

-- Create index for querying import jobs by status and date
CREATE INDEX idx_import_job_status ON import_job(status, executed_at);
