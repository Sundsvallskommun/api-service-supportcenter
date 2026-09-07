package se.sundsvall.supportcenter.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import se.sundsvall.supportcenter.api.validation.impl.ValidHardwareIdentifiersConstraintValidator;

@Documented
@Target({
	ElementType.TYPE
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidHardwareIdentifiersConstraintValidator.class)
public @interface ValidHardwareIdentifiers {
	String message() default "hardwareName and imeiNumber must not be provided at the same time";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
