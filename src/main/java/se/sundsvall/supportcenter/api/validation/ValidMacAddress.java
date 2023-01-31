package se.sundsvall.supportcenter.api.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import se.sundsvall.supportcenter.api.validation.impl.ValidMacAddressConstraintValidator;

@Documented
@Target({ ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidMacAddressConstraintValidator.class)
public @interface ValidMacAddress {
	String message() default "must contain a valid mac address";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
