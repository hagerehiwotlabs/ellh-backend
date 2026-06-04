package com.ellh.infrastructure.cache;

import com.ellh.learning.dto.ContrastiveNote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AIResponseCacheAdapter {
    private static final Logger log = LoggerFactory.getLogger(AIResponseCacheAdapter.class);

    public void cacheContrastiveNote(String key, ContrastiveNote note) {
        // TODO: Implement Redis caching
        log.debug("Caching contrastive note for key: {}", key);
    }
}
