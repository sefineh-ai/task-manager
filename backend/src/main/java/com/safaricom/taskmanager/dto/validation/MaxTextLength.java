package com.safaricom.taskmanager.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Limits the visible text of a rich-text (HTML) value, ignoring the markup. Null is valid. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxTextLengthValidator.class)
public @interface MaxTextLength {

    int value();

    String message() default "text is too long";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
