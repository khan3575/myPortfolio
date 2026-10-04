-- V4 moved each project's old `language` value across as a single entry, but
-- that one column was already being used as a comma-separated list, since it
-- was the only way to record a stack. Those rows still hold the whole list as
-- one tag. V4 has already run, so the split has to happen here rather than
-- by editing it.
--
-- Positions are renumbered over the surviving parts so each project's list
-- stays gapless from 0, which @OrderColumn requires.
CREATE TEMPORARY TABLE split_project_languages AS
SELECT pl.project_id,
       trim(part.value) AS language,
       row_number() OVER (
           PARTITION BY pl.project_id
           ORDER BY pl.position, part.ordinality
       ) - 1 AS position
FROM project_languages pl
CROSS JOIN LATERAL regexp_split_to_table(pl.language, ',')
    WITH ORDINALITY AS part(value, ordinality)
WHERE trim(part.value) <> '';

DELETE FROM project_languages;

INSERT INTO project_languages (project_id, language, position)
SELECT project_id, language, position
FROM split_project_languages;

DROP TABLE split_project_languages;
