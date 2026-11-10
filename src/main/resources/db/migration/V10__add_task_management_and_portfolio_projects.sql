-- Two more project cards. created_at is set just behind the Asset Tagging System
-- row so the home page lists them in this order (newest first) after it.
-- Facts: the Task Management System as described by its author and its repo; this
-- portfolio from its own build.gradle, docker-compose.yml and workflow.
INSERT INTO projects (name, description, source_url, live_url, created_at)
SELECT 'Task Management System',
       'Five-member team project: a task manager built on Servlets, JSP, JDBC and MySQL.',
       'https://github.com/khan3575/Task-Management-System',
       NULL,
       COALESCE((SELECT created_at FROM projects WHERE name = 'Asset Tagging System'), now()) - interval '1 day'
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE source_url = 'https://github.com/khan3575/Task-Management-System');

INSERT INTO project_languages (project_id, language, position)
SELECT p.id, l.language, l.position
FROM projects p
CROSS JOIN (VALUES (0, 'Java'), (1, 'Servlets'), (2, 'JSP'), (3, 'JDBC'), (4, 'MySQL')) AS l(position, language)
WHERE p.source_url = 'https://github.com/khan3575/Task-Management-System'
  AND NOT EXISTS (SELECT 1 FROM project_languages x WHERE x.project_id = p.id);

INSERT INTO project_highlights (project_id, highlight, position)
SELECT p.id, h.highlight, h.position
FROM projects p
CROSS JOIN (VALUES
    (0, 'My part: the shared navbar and sidebar, built against the authenticated session.'),
    (1, 'My part: the add-task notifications and redirects.'),
    (2, 'My part: SLF4J and Logback logging.')
) AS h(position, highlight)
WHERE p.source_url = 'https://github.com/khan3575/Task-Management-System'
  AND NOT EXISTS (SELECT 1 FROM project_highlights x WHERE x.project_id = p.id);

INSERT INTO projects (name, description, source_url, live_url, created_at)
SELECT 'This portfolio site',
       'The site you are reading: a Spring Boot app with an admin panel for projects and blog posts, deployed to a VPS with Docker Compose.',
       'https://github.com/khan3575/myPortfolio',
       'https://sakib-khan.com',
       COALESCE((SELECT created_at FROM projects WHERE name = 'Asset Tagging System'), now()) - interval '2 days'
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE source_url = 'https://github.com/khan3575/myPortfolio');

INSERT INTO project_languages (project_id, language, position)
SELECT p.id, l.language, l.position
FROM projects p
CROSS JOIN (VALUES (0, 'Java'), (1, 'Spring Boot'), (2, 'Spring Security'), (3, 'Thymeleaf'), (4, 'PostgreSQL'),
                   (5, 'Flyway'), (6, 'Docker Compose'), (7, 'GitHub Actions')) AS l(position, language)
WHERE p.source_url = 'https://github.com/khan3575/myPortfolio'
  AND NOT EXISTS (SELECT 1 FROM project_languages x WHERE x.project_id = p.id);

INSERT INTO project_highlights (project_id, highlight, position)
SELECT p.id, h.highlight, h.position
FROM projects p
CROSS JOIN (VALUES
    (0, 'About, skills and experience come from a YAML data file; projects and blog posts live in PostgreSQL under Flyway migrations.'),
    (1, 'Projects and posts are managed from an admin panel behind Spring Security.'),
    (2, 'Medium articles are imported from the RSS feed once an hour and shown on the blog.'),
    (3, 'A push to main triggers a GitHub Actions job that connects to the VPS over SSH and rebuilds the app with Docker Compose.')
) AS h(position, highlight)
WHERE p.source_url = 'https://github.com/khan3575/myPortfolio'
  AND NOT EXISTS (SELECT 1 FROM project_highlights x WHERE x.project_id = p.id);
