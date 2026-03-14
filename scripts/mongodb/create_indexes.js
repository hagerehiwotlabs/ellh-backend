// MongoDB Index Creation Script
// Run against Atlas M0 cluster before first deployment:
//   mongosh "mongodb+srv://..." scripts/mongodb/create_indexes.js
//
// Section 4.5.2.4 — MongoDB Document Schema indexes

// lesson_content collection
db.lesson_content.createIndex(
  { language_code: 1, cefr_level: 1, version_stamp: 1 },
  { name: "idx_lesson_content_lang_cefr_version" }
);

// contrastive_rules collection
db.contrastive_rules.createIndex(
  { source_language: 1, target_language: 1, applicable_lessons: 1 },
  { name: "idx_contrastive_rules_lang_pair_lessons" }
);
db.contrastive_rules.createIndex(
  { is_active: 1 },
  { name: "idx_contrastive_rules_active" }
);

// ai_service_logs collection — TTL index: auto-delete after 90 days
db.ai_service_logs.createIndex(
  { created_at: 1 },
  {
    name: "idx_ai_service_logs_ttl_90days",
    expireAfterSeconds: 7776000  // 90 days in seconds
  }
);

// vocabulary_sets collection
db.vocabulary_sets.createIndex(
  { language_code: 1, cefr_level: 1, category: 1 },
  { name: "idx_vocabulary_sets_lang_cefr_category" }
);

print("All MongoDB indexes created successfully.");
