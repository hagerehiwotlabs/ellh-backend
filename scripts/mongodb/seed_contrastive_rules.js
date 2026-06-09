db.contrastive_rules.deleteMany({}); // Clear placeholders

db.contrastive_rules.insertMany([
  {
    "source_language": "amh",
    "target_language": "tir",
    "applicable_lessons": [1], // Applies to Lesson 1: Greetings & Respect
    "rule_category": "PHONOLOGY",
    "rule_title": "Understanding the Tigrigna 'Qh' Sound (ቐ)",
    "contrast_description": "In Amharic, the letter ቀ (Q) is pronounced as a sharp glottal stop. However, in Tigrigna, when this letter occurs between vowels, it spirantizes into a soft guttural fricative ቐ (Qh), resembling a sound produced deep in the throat. Keep this in mind when practicing speaking!",
    "version_stamp": "1",
    "is_active": true
  },
  {
    "source_language": "amh",
    "target_language": "orm",
    "applicable_lessons": [1], // Applies to Lesson 1: Greetings
    "rule_category": "GRAMMAR",
    "rule_title": "Subject-Object-Verb (SOV) Alignment",
    "contrast_description": "Afaan Oromo and Amharic share the same fundamental Subject-Object-Verb syntactic structure. For example, 'I coffee want' is structured identically in both languages. This means you do not need to relearn word order! You can translate directly word-for-word.",
    "version_stamp": "1",
    "is_active": true
  }
]);

print("Seeded real MongoDB contrastive rules successfully! ✦");
