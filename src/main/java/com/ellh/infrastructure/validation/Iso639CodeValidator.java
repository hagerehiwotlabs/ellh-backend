package com.ellh.infrastructure.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

/**
 * Constraint validator for {@link ValidIso639Code}.
 * Accepts the four ISO 639-3 codes supported by ELLH v1.0.
 */
public class Iso639CodeValidator
        implements ConstraintValidator<ValidIso639Code, String> {

    private static final Set<String> VALID_CODES =
            Set.of("amh", "tir", "orm", "eng");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null) return true; // @NotNull handles null separately
        return VALID_CODES.contains(value.toLowerCase());
    }
}
