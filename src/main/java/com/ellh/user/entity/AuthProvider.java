package com.ellh.user.entity;

/**
 * Authentication provider used for account creation.
 * Section 4.5.2.2 — users.auth_provider column.
 * LOCAL is the default for email/password registration.
 */
public enum AuthProvider {
    LOCAL, GOOGLE, APPLE, FACEBOOK
}
