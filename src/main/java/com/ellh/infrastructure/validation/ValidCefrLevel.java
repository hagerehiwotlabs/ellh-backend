package com.ellh.infrastructure.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that a String is a supported CEFR level for ELLH (A1, A2, B1).
 * Section 1.3.2 — ELLH targets A1 through B1 only.
 */
@Documented
@Constraint(validatedBy = CefrLevelValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCefrLevel {
    String message() default "CEFR level must be one of: A1, A2, B1";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
