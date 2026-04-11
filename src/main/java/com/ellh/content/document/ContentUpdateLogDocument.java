package com.ellh.content.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

/**
 * MongoDB mirror of the PostgreSQL content_update_logs table.
 * Section 4.5.2.4 — content_update_logs MongoDB collection.
 *
 * The PostgreSQL content_update_logs table stores structured metadata (who,
 * when, what action). This MongoDB document stores the full JSONB snapshots
 * (old_values, new_values) which can be arbitrarily large for complex lesson
 * content updates.
 *
 * The two stores are linked by the same content_id string.
 * Neither is a primary source-of-truth — the PostgreSQL row anchors the audit
 * record and this document extends it with rich diff data.
 *
 * This collection is never auto-deleted (no TTL index).
 */
@Document(collection = "content_update_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentUpdateLogDocument {

    @Id
    private String id;

    @Field("pg_log_id")
    private Long pgLogId; // Back-reference to content_update_logs.id in PostgreSQL

    /** LESSON | EXERCISE | LANGUAGE | ACHIEVEMENT | CONTRASTIVE_RULE */
    @Field("content_type")
    private String contentType;

    @Field("content_id")
    private String contentId;

    /** CREATE | UPDATE | DELETE | PUBLISH | DEPRECATE */
    @Field("action")
    private String action;

    @Field("changed_by")
    private Long changedBy; // User.id of the ContentAdmin

    @Field("old_values")
    private Map<String, Object> oldValues; // null for CREATE

    @Field("new_values")
    private Map<String, Object> newValues;

    @Field("changed_at")
    @Builder.Default
    private Instant changedAt = Instant.now();
}
