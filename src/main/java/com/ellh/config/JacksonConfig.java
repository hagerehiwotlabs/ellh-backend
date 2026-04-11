package com.ellh.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson ObjectMapper configuration.
 *
 * Key decisions:
 * - JavaTimeModule: serialises Instant as ISO-8601 strings (not timestamps).
 *   Required for MongoDB @Document classes that store Instant fields.
 * - FAIL_ON_UNKNOWN_PROPERTIES = false: tolerates new fields from future
 *   MongoDB document versions without breaking deserialization.
 * - write-dates-as-timestamps = false: ensures all dates in API responses
 *   are human-readable ISO-8601 strings.
 *
 * Section 4.2 Design Goal a — standardised date format across all API responses.
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Java 8 date/time support (Instant, LocalDate, etc.)
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Do not fail on fields unknown to the current model version
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return mapper;
    }
}
