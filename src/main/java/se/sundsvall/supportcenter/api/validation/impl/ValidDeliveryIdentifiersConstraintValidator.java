package se.sundsvall.supportcenter.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.api.validation.ValidDeliveryIdentifiers;
import se.sundsvall.supportcenter.service.SupportCenterStatus;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.DELIVERED;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.DELIVERED_IT;

/**
 * Advania closes an order with one of three statuses, and each of them demands its own identifiers. Delivered demands
 * serialNumber plus hardwareName or imeiNumber, DeliveredIT demands serialNumber and DeliveredAccessories demands
 * nothing. The status is matched without regard to case and surrounding whitespace, in the same way as CaseService
 * does.
 */
public class ValidDeliveryIdentifiersConstraintValidator implements ConstraintValidator<ValidDeliveryIdentifiers, UpdateCaseRequest> {

	private static final String VALIDATED_NODE_SERIAL_NUMBER = "serialNumber";
	private static final String VALIDATED_NODE_HARDWARE_NAME = "hardwareName";
	private static final String VALIDATED_NODE_IMEI_NUMBER = "imeiNumber";
	private static final String MESSAGE_HARDWARE_IDENTIFIER = "hardwareName or imeiNumber must be provided for the given caseStatus";

	@Override
	public boolean isValid(final UpdateCaseRequest updateCaseRequest, final ConstraintValidatorContext context) {
		if (isNull(updateCaseRequest)) {
			return true;
		}

		final var status = SupportCenterStatus.fromValue(updateCaseRequest.getCaseStatus()).orElse(null);
		final var serialNumberMissing = (status == DELIVERED || status == DELIVERED_IT) && isBlank(updateCaseRequest.getSerialNumber());
		final var hardwareIdentifierMissing = status == DELIVERED && isBlank(updateCaseRequest.getHardwareName()) && isBlank(updateCaseRequest.getImeiNumber());

		if (!serialNumberMissing && !hardwareIdentifierMissing) {
			return true;
		}

		context.disableDefaultConstraintViolation();

		if (serialNumberMissing) {
			addViolation(context, context.getDefaultConstraintMessageTemplate(), VALIDATED_NODE_SERIAL_NUMBER);
		}
		if (hardwareIdentifierMissing) {
			addViolation(context, MESSAGE_HARDWARE_IDENTIFIER, VALIDATED_NODE_HARDWARE_NAME);
			addViolation(context, MESSAGE_HARDWARE_IDENTIFIER, VALIDATED_NODE_IMEI_NUMBER);
		}

		return false;
	}

	private void addViolation(final ConstraintValidatorContext context, final String message, final String node) {
		context.buildConstraintViolationWithTemplate(message)
			.addPropertyNode(node)
			.addConstraintViolation();
	}
}
