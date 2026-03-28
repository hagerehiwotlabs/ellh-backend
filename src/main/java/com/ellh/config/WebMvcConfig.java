package com.ellh.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

/**
 * MVC configuration — content negotiation and path matching.
 * API versioning is handled at the controller level via @RequestMapping("/api/v1/...")
 * not via path suffix or parameter negotiation.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Disable suffix pattern matching — /api/v1/lessons.json is not a valid endpoint
        configurer.setUseRegisteredSuffixPatternMatch(false);
    }
}
