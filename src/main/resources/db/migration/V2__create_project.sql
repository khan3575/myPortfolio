CREATE TABLE projects (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    description       VARCHAR(500),
    url               VARCHAR(500) NOT NULL,
    language          VARCHAR(100),
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_projects_url ON projects (url);
