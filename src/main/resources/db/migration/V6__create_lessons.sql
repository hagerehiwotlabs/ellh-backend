-- V6__create_lessons.sql
CREATE TABLE lessons (
    id                 BIGSERIAL    PRIMARY KEY,
    language_id        BIGINT       NOT NULL REFERENCES languages(id),
    title              VARCHAR(255) NOT NULL,
    description        TEXT,
    cefr_level         VARCHAR(5)   NOT NULL
        CHECK (cefr_level IN ('A1','A2','B1')),
    order_index        INTEGER      NOT NULL
        CHECK (order_index >= 0),
    xp_reward          INTEGER      NOT NULL DEFAULT 25
        CHECK (xp_reward >= 0),
    content_id         VARCHAR(24),
    prerequisites      JSONB,
    estimated_minutes  INTEGER      NOT NULL DEFAULT 8
        CHECK (estimated_minutes > 0),
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (language_id, cefr_level, order_index)
);

COMMENT ON COLUMN lessons.content_id IS
  'MongoDB ObjectId (24-char hex) of the lesson_content document in Atlas.
   NULL until the MongoDB document is created (set atomically in LessonService).';

