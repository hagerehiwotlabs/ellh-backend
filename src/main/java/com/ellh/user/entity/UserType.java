package com.ellh.user.entity;

/**
 * STI discriminator values for the users table.
 * Section 4.5.2.1 — Single Table Inheritance strategy.
 * user_type column CHECK constraint enforces these four values.
 */
public enum UserType {
    FOREIGN_LEARNER,
    BILINGUAL_LEARNER,
    CONTENT_ADMIN,
    SYSTEM_ADMIN
}
