package com.ellh.content.service;

import com.ellh.content.document.ContentUpdateLogDocument;
import com.ellh.content.entity.Lesson;
import com.ellh.content.repository.ContentUpdateLogDocumentRepository;
import com.ellh.feedback.entity.FeedbackReport;
import com.ellh.infrastructure.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Writes to both the PostgreSQL content_update_logs table and the MongoDB
 * content_update_logs collection after every content change.
 * Section 4.5.2.2 — content_update_logs table.
 * Section 4.5.2.4 — content_update_logs MongoDB collection.
 *
 * PostgreSQL stores: who, when, what action, which content item (structured).
 * MongoDB stores: full old_values and new_values snapshots (can be large).
 *
 * Both writes happen in the same service call but NOT in the same transaction
 * (different stores). If the MongoDB write fails, the PostgreSQL write is
 * retained — audit trail is never lost.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentUpdateLogService {

    private final JdbcTemplate                      jdbcTemplate;
    private final ContentUpdateLogDocumentRepository mongoLogRepository;
    private final ObjectMapper                       objectMapper;

    /**
     * Writes a content audit log entry for a lesson create/update/delete.
     *
     * @param contentType  LESSON | EXERCISE | LANGUAGE | ACHIEVEMENT | CONTRASTIVE_RULE
     * @param contentId    String ID (PostgreSQL Long or MongoDB ObjectId)
     * @param action       CREATE | UPDATE | DELETE | PUBLISH | DEPRECATE
     * @param changedBy    User.id of the ContentAdmin
     * @param oldValues    null for CREATE; previous state for UPDATE/DELETE
     * @param newValues    current state after the change
     */
    public void log(String contentType, String contentId, String action,
                    Long changedBy, Object oldValues, Object newValues) {
        // PostgreSQL insert (structured metadata)
        Long pgLogId = writePostgresLog(contentType, contentId, action, changedBy, oldValues, newValues);

        // MongoDB insert (full diff snapshots)
        writeMongoLog(pgLogId, contentType, contentId, action, changedBy, oldValues, newValues);
    }

    private Long writePostgresLog(String contentType, String contentId, String action,
                                   Long changedBy, Object oldValues, Object newValues) {
        try {
            String oldJson = oldValues != null ? objectMapper.writeValueAsString(oldValues) : null;
            String newJson = objectMapper.writeValueAsString(newValues);

            // Use JdbcTemplate to get the generated ID back
            Long id = jdbcTemplate.queryForObject(
                    "INSERT INTO content_update_logs " +
                    "(content_type, content_id, action, changed_by, changed_at, old_values, new_values) " +
                    "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb) RETURNING id",
                    Long.class,
                    contentType, contentId, action, changedBy, Instant.now(), oldJson, newJson);
            log.debug("ContentUpdateLog PostgreSQL row: id={} action={} contentId={}", id, action, contentId);
            return id;
        } catch (Exception e) {
            log.error("Failed to write PostgreSQL content_update_log for contentId={}: {}", contentId, e.getMessage());
            return null;
        }
    }

    private void writeMongoLog(Long pgLogId, String contentType, String contentId,
                                String action, Long changedBy, Object oldValues, Object newValues) {
        try {
            Map<String, Object> oldMap = oldValues != null
                    ? objectMapper.convertValue(oldValues, Map.class) : null;
            Map<String, Object> newMap = objectMapper.convertValue(newValues, Map.class);

            ContentUpdateLogDocument doc = ContentUpdateLogDocument.builder()
                    .pgLogId(pgLogId)
                    .contentType(contentType)
                    .contentId(contentId)
                    .action(action)
                    .changedBy(changedBy)
                    .oldValues(oldMap)
                    .newValues(newMap)
                    .changedAt(Instant.now())
                    .build();
            mongoLogRepository.save(doc);
        } catch (Exception e) {
            // MongoDB write failure is non-fatal — PostgreSQL log is the primary record
            log.warn("Failed to write MongoDB content_update_log for contentId={}: {}", contentId, e.getMessage());
        }
    }
}
