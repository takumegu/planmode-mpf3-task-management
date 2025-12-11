-- Create project table
CREATE TABLE project (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    start_date DATE,
    end_date DATE,
    status VARCHAR(32) NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'archived')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Create index on project name for search performance
CREATE INDEX idx_project_name ON project(name);
