package com.ellh.config;

import com.mongodb.WriteConcern;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.WriteConcernResolver;

/**
 * MongoDB configuration.
 * Section 4.5.2.4 — MongoDB collections and write concern.
 * Section 4.5.2.6 — Cross-store consistency requires MAJORITY write concern
 * on lesson_content inserts so the content_id bridge never references a
 * document that was not durably written.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {

    /**
     * WriteConcernResolver — lesson_content and contrastive_rules operations
     * use MAJORITY write concern; ai_service_logs use ACKNOWLEDGED (faster,
     * acceptable for audit logs that are not business-critical).
     */
    @Bean
    public WriteConcernResolver writeConcernResolver() {
        return action -> {
            String collection = action.getCollectionName();
            if ("lesson_content".equals(collection)
                    || "contrastive_rules".equals(collection)) {
                return WriteConcern.MAJORITY;
            }
            return WriteConcern.ACKNOWLEDGED;
        };
    }

    /**
     * MongoTransactionManager — enables @Transactional on MongoDB operations.
     * Required for the content_id bridge compensating transaction pattern
     * (Section 4.5.2.6).
     * NOTE: MongoDB Atlas M0 free tier does NOT support multi-document transactions.
     * This bean is present so the code compiles and works in paid tiers.
     * On M0, compensating transactions are handled at the application level.
     */
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
        return new MongoTransactionManager(factory);
    }
}
