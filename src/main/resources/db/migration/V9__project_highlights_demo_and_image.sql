-- Richer project cards: a few short bullets, an optional demo login and an
-- optional screenshot. All three are optional, so existing rows are unaffected.
ALTER TABLE projects ADD COLUMN demo_login VARCHAR(500);
ALTER TABLE projects ADD COLUMN image_url  VARCHAR(500);

CREATE TABLE project_highlights (
    project_id  BIGINT       NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    highlight   VARCHAR(500) NOT NULL,
    position    INT          NOT NULL,
    PRIMARY KEY (project_id, position)
);

-- Asset Tagging System. Facts are from its README and docs/SETUP.md; the demo
-- accounts are the public, fictional ones from db/seed/V1000__dev_seed_data.sql.
UPDATE projects
SET demo_login = 'Admin: mehedi@gmail.com or employee: sakib@gmail.com — password DemoOnly2026!',
    image_url  = '/img/projects/asset-tagging-system.png'
WHERE name = 'Asset Tagging System';

INSERT INTO project_highlights (project_id, highlight, position)
SELECT p.id, h.highlight, h.position
FROM projects p
CROSS JOIN (VALUES
    (0, 'Three rules are enforced by database constraints rather than application checks: one open approval per asset, one active custody record per asset, and one sign-off per user per approval.'),
    (1, 'Append-only activity log of every action, refusals included, with a correlation ID that groups the rows one action produced.'),
    (2, 'Schema managed with Flyway migrations.'),
    (3, 'GitHub Actions pipeline builds the project with Maven and deploys it to the VPS on every push to main.')
) AS h(position, highlight)
WHERE p.name = 'Asset Tagging System';
