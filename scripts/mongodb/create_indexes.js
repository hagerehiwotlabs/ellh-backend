// MongoDB Index Creation Script — Sprint 2
// Run against Atlas M0 cluster BEFORE first deployment:
//   mongosh "mongodb+srv://ellh_app:[PASSWORD]@ellh-cluster.xxxxx.mongodb.net/ellh" \
//           scripts/mongodb/create_indexes.js
//
// IMPORTANT: Run this ONCE on the Atlas cluster. Re-running is safe —
// MongoDB createIndex() is idempotent (skips if index already exists).
//
// Section 4.5.2.4 — MongoDB Document Schema index specification
// ============================================================

// ── lesson_content ──────────────────────────────────────────────────────────
// Compound index: lesson dashboard queries always filter by language + level
db.lesson_content.createIndex(
    { language_code: 1, cefr_level: 1, version_stamp: 1 },
    { name: "idx_lesson_content_lang_cefr_version", background: true }
);

// lesson_id lookup — used by LessonContentRepository.findByLessonId()
db.lesson_content.createIndex(
    { lesson_id: 1 },
    { name: "idx_lesson_content_lesson_id", unique: true, background: true }
);

// is_active filter — used by admin content listing
db.lesson_content.createIndex(
    { is_active: 1 },
    { name: "idx_lesson_content_active", background: true }
);

// ── vocabulary_sets ─────────────────────────────────────────────────────────
// Practice tab filter: language + CEFR + category
db.vocabulary_sets.createIndex(
    { language_code: 1, cefr_level: 1, category: 1 },
    { name: "idx_vocabulary_sets_lang_cefr_category", background: true }
);

// ── contrastive_rules ───────────────────────────────────────────────────────
// Primary query for ContrastiveAnalysisEngine (Sprint 6):
// { source_language: "amh", target_language: "tir",
//   applicable_lessons: lessonId, is_active: true }
// The array field applicable_lessons uses a multikey index automatically.
db.contrastive_rules.createIndex(
    { source_language: 1, target_language: 1, applicable_lessons: 1 },
    { name: "idx_contrastive_rules_lang_pair_lessons", background: true }
);

db.contrastive_rules.createIndex(
    { is_active: 1 },
    { name: "idx_contrastive_rules_active", background: true }
);

// version_stamp — used for cache staleness detection by Android clients
db.contrastive_rules.createIndex(
    { version_stamp: 1 },
    { name: "idx_contrastive_rules_version", background: true }
);

// ── ai_service_logs ─────────────────────────────────────────────────────────
// TTL index: auto-delete documents older than 90 days (7,776,000 seconds).
// Section 4.5.2.4 — ai_service_logs TTL specification.
// IMPORTANT: TTL index requires the field to be a Date/ISODate — Instant
// is stored as Date in MongoDB BSON. Verify after creation in Atlas UI:
//   Atlas → Clusters → ellh-cluster → Collections → ai_service_logs
//   → Indexes tab → confirm expireAfterSeconds is 7776000
db.ai_service_logs.createIndex(
    { created_at: 1 },
    {
        name: "idx_ai_service_logs_ttl_90days",
        expireAfterSeconds: 7776000,
        background: true
    }
);

// user_id + status — FeedbackReporter failure rate check
db.ai_service_logs.createIndex(
    { provider: 1, status: 1, created_at: 1 },
    { name: "idx_ai_service_logs_provider_status_date", background: true }
);

// ── content_update_logs ─────────────────────────────────────────────────────
// content_id lookup — admin audit history for a specific content item
db.content_update_logs.createIndex(
    { content_id: 1, changed_at: -1 },
    { name: "idx_content_update_logs_content_date", background: true }
);

db.content_update_logs.createIndex(
    { changed_by: 1, changed_at: -1 },
    { name: "idx_content_update_logs_user_date", background: true }
);

// ── Verification ─────────────────────────────────────────────────────────────
print("\n=== Index Creation Complete ===");
print("lesson_content indexes:      " + db.lesson_content.getIndexes().length);
print("vocabulary_sets indexes:     " + db.vocabulary_sets.getIndexes().length);
print("contrastive_rules indexes:   " + db.contrastive_rules.getIndexes().length);
print("ai_service_logs indexes:     " + db.ai_service_logs.getIndexes().length);
print("content_update_logs indexes: " + db.content_update_logs.getIndexes().length);
print("\nMANUAL VERIFICATION REQUIRED:");
print("  1. Open Atlas UI → Clusters → ellh-cluster → Collections");
print("  2. Select ai_service_logs → Indexes tab");
print("  3. Confirm idx_ai_service_logs_ttl_90days has TTL = 7776000 seconds");
print("  4. Confirm idx_lesson_content_lesson_id is UNIQUE");
print("  Run this command to verify TTL index:");
print("  db.ai_service_logs.getIndexes().find(i => i.expireAfterSeconds)");
