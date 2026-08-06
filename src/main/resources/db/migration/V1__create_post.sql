CREATE TABLE posts (
    id                BIGSERIAL PRIMARY KEY,
    title             VARCHAR(255) NOT NULL,
    slug              VARCHAR(255) NOT NULL,
    summary           VARCHAR(500),
    content_markdown  TEXT NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at      TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_posts_slug ON posts (slug);
CREATE INDEX idx_posts_status_published_at ON posts (status, published_at DESC);
