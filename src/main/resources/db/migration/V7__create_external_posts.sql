-- Articles published somewhere else (Medium, mostly) that the blog links out
-- to. Kept apart from `posts`: an on-site post needs Markdown content and a
-- slug, and one of these has neither -- only what a card shows and where it
-- leads.
CREATE TABLE external_posts (
    id            BIGSERIAL PRIMARY KEY,
    source        VARCHAR(20)   NOT NULL,
    url           VARCHAR(1000) NOT NULL,
    title         VARCHAR(255)  NOT NULL,
    summary       VARCHAR(500),
    image_url     VARCHAR(1000),
    published_at  TIMESTAMP     NOT NULL,
    -- Off the public pages without losing the row; a deleted feed post would
    -- only be imported again on the next sync.
    hidden        BOOLEAN       NOT NULL DEFAULT FALSE,
    -- Set once an admin edits the card, so the feed stops overwriting it.
    locked        BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP     NOT NULL DEFAULT now()
);

-- The URL is how a feed item finds its row again on every sync.
CREATE UNIQUE INDEX idx_external_posts_url ON external_posts (url);
CREATE INDEX idx_external_posts_hidden_published_at ON external_posts (hidden, published_at DESC);
