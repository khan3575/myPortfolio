-- A "github-url: <repo>.git" token was pasted into some project descriptions and
-- printed on the card as raw text. Move it into source_url (without ".git") and
-- strip it from the description. Rows without the token are untouched; an
-- existing source_url is never overwritten.
UPDATE projects
SET source_url  = COALESCE(source_url,
                           regexp_replace(substring(description from '(?i)github-url:\s*(\S+)'), '(\.git)?/?$', '')),
    description = NULLIF(trim(regexp_replace(description, '\s*github-url:\s*\S+', '', 'gi')), '')
WHERE description ~* 'github-url:';
