-- The CV is one small PDF replaced a few times a year. Keeping the bytes in
-- Postgres rather than object storage keeps the download on the site's own
-- domain -- a stable /cv link that survives every re-upload -- and leaves the
-- app with one less credential to configure before it can serve one.
CREATE TABLE cv_documents (
    id            BIGSERIAL     PRIMARY KEY,
    filename      VARCHAR(255)  NOT NULL,
    content_type  VARCHAR(100)  NOT NULL,
    size_bytes    BIGINT        NOT NULL,
    data          BYTEA         NOT NULL,
    uploaded_at   TIMESTAMP     NOT NULL DEFAULT now()
);
