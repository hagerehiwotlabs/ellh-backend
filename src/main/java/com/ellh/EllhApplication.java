package com.ellh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // Required for: GDPR cleanup job (Sprint 9), streak reminders (Sprint 8)
public class EllhApplication {
    public static void main(String[] args) {
        SpringApplication.run(EllhApplication.class, args);
    }
}
