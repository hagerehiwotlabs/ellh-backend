package com.ellh.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

/**
 * Redis configuration — Lettuce client with TLS support (production).
 * Section 4.5.5.5 — Redis connection is TLS-enforced in prod via rediss:// URL.
 * Section 4.5.4.4 — TTL configuration per cache type is set at write time
 * in SessionCacheService and CacheEvictionService (not globally here).
 *
 * Serialisation: String keys, String values (JSON serialised by service layer).
 * Using StringRedisSerializer avoids class name prefixes in Redis keys,
 * making keys human-readable in Redis Cloud monitoring dashboard.
 */
@Slf4j
@Configuration
public class RedisConfig {

    /**
     * Primary RedisTemplate used by SessionCacheService and CacheEvictionService.
     * Keys and values are plain UTF-8 strings.
     * Services serialize objects to JSON via ObjectMapper before calling set().
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();

        log.info("RedisTemplate configured with StringRedisSerializer");
        return template;
    }
}
