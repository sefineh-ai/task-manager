package com.safaricom.taskmanager.dto.validation;

import com.safaricom.taskmanager.util.HtmlSanitizer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MaxTextLengthValidator implements ConstraintValidator<MaxTextLength, String> {

    private int max;

    @Override
    public void initialize(MaxTextLength annotation) {
        this.max = annotation.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return HtmlSanitizer.textLength(value) <= max;
    }
}
