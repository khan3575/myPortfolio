ALTER TABLE projects RENAME COLUMN url TO source_url;
ALTER TABLE projects ALTER COLUMN source_url DROP NOT NULL;
ALTER TABLE projects ADD COLUMN live_url VARCHAR(500);

ALTER INDEX idx_projects_url RENAME TO idx_projects_source_url;
