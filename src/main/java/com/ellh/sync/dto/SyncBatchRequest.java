package com.ellh.sync.dto;

import lombok.Data;
import java.util.List;

@Data
public class SyncBatchRequest {
    private String batchId;
    private List<SyncEventDto> events;

    @Data
    public static class SyncEventDto {
        private String idempotencyKey;
        private String actionType; // EXERCISE_COMPLETE, LESSON_COMPLETE, FEEDBACK_SUBMIT
        private java.util.Map<String, Object> payload;
        private long createdAt;
    }
}
