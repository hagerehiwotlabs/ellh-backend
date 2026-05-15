package com.ellh.infrastructure.schedule;

import com.ellh.feedback.service.FeedbackReporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Hourly Redis cache memory monitor.
 *
 * Logs Redis memory usage and alerts via FeedbackReporter when usage
 * exceeds 80% of the free-tier limit (30MB on Redis Cloud free tier).
 *
 * This monitor was explicitly planned in Sprint 2:
 * "Redis Cloud free tier memory limit (30MB) — monitor during seed data phase;
 * implement cache size monitoring in Sprint 9" (Section 4.4 Sprint 2 risks).
 *
 * Alert strategy (Section 4.4 Trade-off c):
 *   "Automated monitoring alerts the development team when usage
 *   approaches 80% of any free-tier limit."
 *
 * Alert threshold: configurable via app config (default 80% of 30MB = 24MB).
 * Severity: MEDIUM (performance degradation, not data loss).
 *
 * Section 4.4 Trade-off c, Sprint 2 risk R-10.
 */
@Component
public class RedisCacheMonitor {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheMonitor.class);

    /** Redis Cloud free tier memory limit in bytes (30MB). */
    private static final long FREE_TIER_LIMIT_BYTES = 30L * 1024 * 1024;

    /** Alert when usage exceeds this fraction of the free-tier limit. */
    @Value("${ellh.cache.alert-threshold-percent:80}")
    private int alertThresholdPercent;

    private final RedisConnectionFactory redisConnectionFactory;
    private final FeedbackReporter       feedbackReporter;

    public RedisCacheMonitor(RedisConnectionFactory redisConnectionFactory,
                             FeedbackReporter feedbackReporter) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.feedbackReporter       = feedbackReporter;
    }

    /**
     * Runs on startup and then every hour.
     * Reads Redis INFO MEMORY, extracts used_memory_bytes, logs it, and
     * fires alert if above threshold.
     */
    @Scheduled(fixedRateString = "3600000", initialDelayString = "60000")
    public void checkCacheMemory() {
        try {
            Properties info = redisConnectionFactory.getConnection()
                    .serverCommands()
                    .info("memory");

            if (info == null) {
                log.warn("RedisCacheMonitor: could not retrieve Redis INFO MEMORY");
                return;
            }

            String usedMemoryStr = info.getProperty("used_memory");
            if (usedMemoryStr == null) return;

            long usedBytes = Long.parseLong(usedMemoryStr.trim());
            double usedMb  = usedBytes / (1024.0 * 1024.0);
            int    usedPct = (int) ((usedBytes * 100L) / FREE_TIER_LIMIT_BYTES);

            log.info("RedisCacheMonitor: used={:.2f}MB ({}% of 30MB free-tier limit)",
                    usedMb, usedPct);

            if (usedPct >= alertThresholdPercent) {
                log.warn("RedisCacheMonitor: ALERT — Redis usage at {}% (threshold={}%)",
                        usedPct, alertThresholdPercent);
                feedbackReporter.reportCacheAlert(usedPct, alertThresholdPercent, usedMb);
            }

        } catch (Exception e) {
            log.error("RedisCacheMonitor: failed to check Redis memory", e);
            // Non-critical: monitoring failure does not affect application operation
        }
    }
}
