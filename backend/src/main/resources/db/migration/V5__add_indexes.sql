-- Additional performance indexes based on query patterns

-- Index on task status for filtering
CREATE INDEX idx_task_status ON task(status);

-- Partial index on task_code for faster lookups when task_code is not null
CREATE INDEX idx_task_code ON task(task_code) WHERE task_code IS NOT NULL;
