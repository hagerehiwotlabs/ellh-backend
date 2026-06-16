package com.ellh.sync.service;

import com.ellh.learning.entity.UserProgress;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ConflictResolver {

    /**
     * Conflict resolution logic.
     * Enforces: "COMPLETED" > "IN_PROGRESS", then Highest Score wins, then Latest Timestamp wins.
     */
    public UserProgress resolve(UserProgress local, UserProgress remote) {
        if (local == null) return remote;
        if (remote == null) return local;

        // 1. Completion Priority
        if ("COMPLETED".equals(local.getStatus()) && !"COMPLETED".equals(remote.getStatus())) {
            return local;
        }
        if (!"COMPLETED".equals(local.getStatus()) && "COMPLETED".equals(remote.getStatus())) {
            return remote;
        }

        // 2. Highest Score Wins
        int localScore = local.getScore() != null ? local.getScore() : 0;
        int remoteScore = remote.getScore() != null ? remote.getScore() : 0;

        if (localScore > remoteScore) return local;
        if (remoteScore > localScore) return remote;

        // 3. Latest Timestamp Wins
        Instant localTime = local.getLastAttemptAt() != null ? local.getLastAttemptAt() : Instant.EPOCH;
        Instant remoteTime = remote.getLastAttemptAt() != null ? remote.getLastAttemptAt() : Instant.EPOCH;

        return localTime.isAfter(remoteTime) ? local : remote;
    }
}
