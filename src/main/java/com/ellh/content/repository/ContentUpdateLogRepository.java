package com.ellh.content.repository;

import java.time.LocalDateTime;

public interface ContentUpdateLogRepository {
    void logGdprPurge(int deletedAttempts, int deletedUsers, LocalDateTime timestamp);
}
