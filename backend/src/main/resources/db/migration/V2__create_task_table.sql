-- Create task table
CREATE TABLE task (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    task_code VARCHAR(64),
    name VARCHAR(255) NOT NULL,
    assignee VARCHAR(120),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    progress SMALLINT NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
    status VARCHAR(32) NOT NULL DEFAULT 'planned'
        CHECK (status IN ('planned', 'in_progress', 'done', 'blocked', 'on_hold')),
    parent_task_id BIGINT REFERENCES task(id),
    is_milestone BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT task_dates_valid CHECK (start_date <= end_date),
    CONSTRAINT task_code_unique UNIQUE (project_id, task_code)
);

-- Create indexes for common query patterns
CREATE INDEX idx_task_project ON task(project_id);
CREATE INDEX idx_task_parent ON task(parent_task_id);
CREATE INDEX idx_task_dates ON task(start_date, end_date);
