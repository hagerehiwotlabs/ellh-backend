package com.ellh.infrastructure.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

/** Constraint validator for {@link ValidCefrLevel}. */
public class CefrLevelValidator
        implements ConstraintValidator<ValidCefrLevel, String> {

    private static final Set<String> VALID_LEVELS = Set.of("A1", "A2", "B1");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null) return true;
        return VALID_LEVELS.contains(value.toUpperCase());
    }
}
