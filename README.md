# myPortfolio

Spring Boot app that will back the dynamic version of [sakib-khan.com](https://sakib-khan.com), replacing the current static landing page. Not yet deployed — in active development.

Full system design (architecture, data flow, decision log) is documented separately; this repo currently implements the data layer:

- **Static content** (Hero, About, Skills, Experience, Education, Certifications, Awards, Contact) — `portfolio-data.yml`, bound via `@ConfigurationProperties` records. No database for content that changes a few times a year.
- **Blog** — `Post` entity, PostgreSQL, Flyway-managed schema.
- **Projects** — not yet implemented; will be fetched live from the GitHub API with a cached, config-driven fallback.

## Tech stack

Java 25, Spring Boot 4.1, Spring Data JPA, Spring Security, Thymeleaf, PostgreSQL, Flyway, Docker

## Getting started

Requires a running PostgreSQL instance:

```bash
docker run -d --name portfolio-db -e POSTGRES_DB=portfolio -e POSTGRES_PASSWORD=devpass -p 5432:5432 postgres:16-alpine

SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/portfolio \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=devpass \
./gradlew bootRun
```
