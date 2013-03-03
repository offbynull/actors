package com.offbynull.peernetic.chord.messages.validation;


import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public final class NotNullSetElementsValidator
        implements ConstraintValidator<NotNullSetElements, Set<?>> {

    @Override
    public void initialize(NotNullSetElements constraintAnnotation) {
    }

    @Override
    public boolean isValid(Set<?> value, ConstraintValidatorContext context) {
        return !value.contains(null);
    }

}