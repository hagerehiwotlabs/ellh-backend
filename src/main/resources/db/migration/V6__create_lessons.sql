-- V6: lessons — lesson metadata (content stored in MongoDB via content_id bridge)
-- Section 4.5.2.2 — lessons table specification
-- Section 4.5.2.6 — content_id is the cross-store bridge to MongoDB lesson_content

CREATE TABLE lessons (
    id                  BIGSERIAL       PRIMARY KEY,
    language_id         BIGINT          NOT NULL REFERENCES languages(id) ON DELETE RESTRICT,
    title               VARCHAR(255)    NOT NULL,
    description         TEXT,
    cefr_level          VARCHAR(5)      NOT NULL
                            CHECK (cefr_level IN ('A1', 'A2', 'B1')),
    order_index         INTEGER         NOT NULL,
    xp_reward           INTEGER         NOT NULL DEFAULT 25,
    content_id          VARCHAR(24),    -- MongoDB ObjectId string (24 hex chars)
    prerequisites       JSONB,          -- JSON array of prerequisite lesson IDs
    estimated_minutes   INTEGER         NOT NULL DEFAULT 8,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN lessons.content_id IS 'MongoDB ObjectId — cross-store bridge to lesson_content collection (Section 4.5.2.6)';
COMMENT ON COLUMN lessons.prerequisites IS 'JSONB array of lesson IDs that must be COMPLETED before this lesson unlocks';
