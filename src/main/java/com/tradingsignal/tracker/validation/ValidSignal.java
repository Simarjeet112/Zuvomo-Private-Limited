package com.tradingsignal.tracker.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})       // applies to a whole class, not a single field
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidSignalValidator.class)
public @interface ValidSignal {
    String message() default "Invalid signal";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}