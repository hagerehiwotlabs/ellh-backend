package com.ellh.config;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * OkHttpClient configuration for outbound HTTP calls made by the AI Gateway.
 * Section 4.5.5.6 — Network timeout configuration:
 *   connectTimeout: 10s
 *   readTimeout:    30s (standard) / 60s (AI endpoints, set per-call)
 *   writeTimeout:   15s
 *   AI call timeout: 5s (enforced at AIServiceGateway level, not OkHttp)
 *
 * This bean is injected by GoogleColabAIService and HuggingFaceAIService
 * in Sprint 5. It is declared here so the Spring context validates at startup
 * and the config is not buried in AI-domain code.
 *
 * The AI-specific 5s timeout (Section 4.5.4.4 circuit breaker) is NOT set
 * here — it is applied per-call via OkHttpClient.newBuilder().callTimeout()
 * in GoogleColabAIService to allow different timeouts per provider.
 */
@Configuration
public class OkHttpConfig {

    @Value("${ai.gateway.timeout-seconds:5}")
    private int aiTimeoutSeconds;

    @Bean
    public OkHttpClient okHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        // Log request/response in dev; NONE in prod (controlled by log level)
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                // Retry once on connection failure (not on 5xx — that is the
                // circuit breaker's responsibility)
                .retryOnConnectionFailure(true)
                .addInterceptor(logging)
                .build();
    }
}
