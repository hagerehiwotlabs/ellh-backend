package com.ellh.infrastructure.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that a String is one of the supported ISO 639-3 language codes.
 * Supported: amh (Amharic), tir (Tigrigna), orm (Afaan Oromo), eng (English).
 * Used on API request DTOs wherever a language code field appears.
 * Section 4.2 Design Goal a — ISO 639-3 codes in all API responses.
 */
@Documented
@Constraint(validatedBy = Iso639CodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidIso639Code {
    String message() default "Language code must be one of: amh, tir, orm, eng";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
