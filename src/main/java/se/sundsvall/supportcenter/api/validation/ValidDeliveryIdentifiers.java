package se.sundsvall.supportcenter.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import se.sundsvall.supportcenter.api.validation.impl.ValidDeliveryIdentifiersConstraintValidator;

@Documented
@Target({
	ElementType.TYPE
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidDeliveryIdentifiersConstraintValidator.class)
public @interface ValidDeliveryIdentifiers {
	String message() default "must be provided for the given caseStatus";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
