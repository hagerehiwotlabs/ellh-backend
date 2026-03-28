package com.ellh.user.entity;

/**
 * Lifecycle state of a user account.
 * Section 4.5.2.2 — users.account_status column.
 * PENDING_VERIFICATION is the default on registration.
 * INACTIVE is set on account deletion request (GDPR EC-08 Step 1).
 */
public enum AccountStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    PENDING_VERIFICATION
}
