package com.ellh.ai.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Thread-Safe AI Request Deduplicator.
 * Prevents duplicate processing of identical audio hashes on concurrent uploads.
 */
@Slf4j
@Component
public class RequestDeduplicator {

    // Store active locks keyed by the SHA-256 audio hash
    private final Map<String, DeduplicationLock> activeRequests = new ConcurrentHashMap<>();

    private static class DeduplicationLock {
        final CountDownLatch latch = new CountDownLatch(1);
        volatile Object cachedResult = null;
        volatile String errorMessage = null;
    }

    /**
     * Checks if a request with this hash is already running.
     * If yes, blocks the thread (up to 10s) and returns the completed result of the first thread.
     * If no, locks the hash and allows the current thread to proceed as the primary worker.
     *
     * @param audioHash SHA-256 of the raw audio bytes
     * @return Cached result if duplicate, null if first worker
     */
    public Object registerOrWait(String audioHash) throws InterruptedException {
        DeduplicationLock lock = new DeduplicationLock();
        DeduplicationLock existing = activeRequests.putIfAbsent(audioHash, lock);

        if (existing != null) {
            log.info("Deduplicator: Duplicate request detected for hash: {}. Blocking thread...", audioHash);
            
            // Wait for the primary thread to finish processing
            boolean completed = existing.latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("Deduplicator: Timed out waiting for primary request hash: {}", audioHash);
                throw new InterruptedException("Deduplication timeout");
            }

            if (existing.errorMessage != null) {
                throw new RuntimeException("Primary request failed: " + existing.errorMessage);
            }

            log.info("Deduplicator: Reusing computed result for hash: {}", audioHash);
            return existing.cachedResult;
        }

        return null; // Primary thread allowed to proceed
    }

    /**
     * Unlocks the hash and broadcasts the computed result to all blocked threads.
     */
    public void release(String audioHash, Object result) {
        DeduplicationLock lock = activeRequests.remove(audioHash);
        if (lock != null) {
            lock.cachedResult = result;
            lock.latch.countDown(); // Release all awaiting threads
        }
    }

    /**
     * Unlocks the hash and broadcasts the error to all blocked threads.
     */
    public void releaseWithError(String audioHash, String error) {
        DeduplicationLock lock = activeRequests.remove(audioHash);
        if (lock != null) {
            lock.errorMessage = error;
            lock.latch.countDown();
        }
    }
}
