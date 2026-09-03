-- A project is rarely one language; the single `language` column could only
-- ever hold GitHub's primary one, and nothing at all for a hand-added project
-- built out of two or three.
CREATE TABLE project_languages (
    project_id  BIGINT       NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    language    VARCHAR(100) NOT NULL,
    position    INT          NOT NULL,
    PRIMARY KEY (project_id, position)
);

INSERT INTO project_languages (project_id, language, position)
SELECT id, language, 0
FROM projects
WHERE language IS NOT NULL AND language <> '';

ALTER TABLE projects DROP COLUMN language;
