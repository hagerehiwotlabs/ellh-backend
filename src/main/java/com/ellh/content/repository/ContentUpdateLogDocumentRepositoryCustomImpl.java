package com.ellh.content.repository;

import com.ellh.content.document.ContentUpdateLogDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ContentUpdateLogDocumentRepositoryCustomImpl
        implements ContentUpdateLogDocumentRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public void logGdprPurge(int deletedAttempts, int deletedUsers, LocalDateTime timestamp) {
        // Build audit document matching your existing schema
        ContentUpdateLogDocument log = ContentUpdateLogDocument.builder()
                .action("GDPR_PURGE")
                .contentType("SYSTEM")
                .contentId("GDPR_PURGE")               // not tied to any content
                .changedBy(null)                       // no user – system action
                .changedAt(timestamp.toInstant(ZoneOffset.UTC))
                .oldValues(null)                        // nothing was overwritten
                .newValues(buildPurgePayload(deletedAttempts, deletedUsers))
                .build();

        mongoTemplate.save(log);
    }

    private Map<String, Object> buildPurgePayload(int deletedAttempts, int deletedUsers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("deleted_attempts", deletedAttempts);
        payload.put("deleted_users", deletedUsers);
        payload.put("description", String.format(
                "GDPR purge: %d attempts deleted, %d users fully deleted.",
                deletedAttempts, deletedUsers));
        return payload;
    }
}
