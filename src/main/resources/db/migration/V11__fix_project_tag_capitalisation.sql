-- "Mysql" and "Spring boot" were typed by hand; fix the stored tags.
UPDATE project_languages SET language = 'MySQL'       WHERE lower(language) = 'mysql'       AND language <> 'MySQL';
UPDATE project_languages SET language = 'Spring Boot' WHERE lower(language) = 'spring boot' AND language <> 'Spring Boot';
