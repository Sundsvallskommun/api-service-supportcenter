package se.sundsvall.supportcenter.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.api.validation.ValidHardwareIdentifiers;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Both hardwareName and imeiNumber are mapped to the same attribute in POB (Virtual.Shop_CI_Name), which means that the
 * last one written silently overwrites the other. Reject requests where both are provided.
 */
public class ValidHardwareIdentifiersConstraintValidator implements ConstraintValidator<ValidHardwareIdentifiers, UpdateCaseRequest> {

	private static final String VALIDATED_NODE_HARDWARE_NAME = "hardwareName";
	private static final String VALIDATED_NODE_IMEI_NUMBER = "imeiNumber";

	@Override
	public boolean isValid(final UpdateCaseRequest updateCaseRequest, final ConstraintValidatorContext context) {
		if (isNull(updateCaseRequest)) {
			return true;
		}

		if (nonNull(updateCaseRequest.getHardwareName()) && nonNull(updateCaseRequest.getImeiNumber())) {
			addViolations(context);
			return false;
		}

		return true;
	}

	private void addViolations(final ConstraintValidatorContext context) {
		context.disableDefaultConstraintViolation();
		final var message = context.getDefaultConstraintMessageTemplate();

		context.buildConstraintViolationWithTemplate(message)
			.addPropertyNode(VALIDATED_NODE_HARDWARE_NAME)
			.addConstraintViolation();

		context.buildConstraintViolationWithTemplate(message)
			.addPropertyNode(VALIDATED_NODE_IMEI_NUMBER)
			.addConstraintViolation();
	}
}
