-- Create task_dependency table
CREATE TABLE task_dependency (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES task(id) ON DELETE CASCADE,
    predecessor_task_id BIGINT NOT NULL REFERENCES task(id) ON DELETE CASCADE,
    type VARCHAR(8) NOT NULL DEFAULT 'FS' CHECK (type IN ('FS', 'SS', 'FF', 'SF')),
    CONSTRAINT dependency_unique UNIQUE (task_id, predecessor_task_id),
    CONSTRAINT dependency_self_ref CHECK (task_id <> predecessor_task_id)
);

-- Create indexes for dependency queries
CREATE INDEX idx_dependency_task ON task_dependency(task_id);
CREATE INDEX idx_dependency_predecessor ON task_dependency(predecessor_task_id);
