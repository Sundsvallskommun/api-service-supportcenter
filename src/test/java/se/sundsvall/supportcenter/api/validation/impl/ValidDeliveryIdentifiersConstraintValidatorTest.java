package se.sundsvall.supportcenter.api.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidDeliveryIdentifiersConstraintValidatorTest {

	private static final String MESSAGE = "must be provided for the given caseStatus";
	private static final String MESSAGE_HARDWARE_IDENTIFIER = "hardwareName or imeiNumber must be provided for the given caseStatus";
	private static final String VALIDATED_NODE_SERIAL_NUMBER = "serialNumber";
	private static final String VALIDATED_NODE_HARDWARE_NAME = "hardwareName";
	private static final String VALIDATED_NODE_IMEI_NUMBER = "imeiNumber";

	@Mock
	private ConstraintValidatorContext contextMock;

	@Mock
	private ConstraintViolationBuilder builderMock;

	@Mock
	private NodeBuilderCustomizableContext nodeBuilderMock;

	private final ValidDeliveryIdentifiersConstraintValidator validator = new ValidDeliveryIdentifiersConstraintValidator();

	@Test
	void testNullRequest() {
		assertThat(validator.isValid(null, contextMock)).isTrue();

		verifyNoInteractions(contextMock, builderMock, nodeBuilderMock);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
		"Resolved", "Delivered - Action Needed", "In Process", "Delivered IT", "DeliveredAccessories", " deliveredaccessories "
	})
	void testStatusWithoutRequirements(final String caseStatus) {
		assertThat(validator.isValid(UpdateCaseRequest.create().withCaseStatus(caseStatus), contextMock)).isTrue();

		verifyNoInteractions(contextMock, builderMock, nodeBuilderMock);
	}

	@ParameterizedTest
	@CsvSource(nullValues = "null", value = {
		"Delivered, FRGDZ1J, WB12345NY, null",
		"Delivered, FRGDZ1J, null, 702548572062338",
		"delivered, FRGDZ1J, WB12345NY, null",
		"DeliveredIT, FRGDZ1J, null, null",
		"DELIVEREDIT, FRGDZ1J, null, null"
	})
	void testDeliveryIdentifiersProvided(final String caseStatus, final String serialNumber, final String hardwareName, final String imeiNumber) {
		final var updateCaseRequest = UpdateCaseRequest.create()
			.withCaseStatus(caseStatus)
			.withSerialNumber(serialNumber)
			.withHardwareName(hardwareName)
			.withImeiNumber(imeiNumber);

		assertThat(validator.isValid(updateCaseRequest, contextMock)).isTrue();

		verifyNoInteractions(contextMock, builderMock, nodeBuilderMock);
	}

	@ParameterizedTest
	@CsvSource(nullValues = "null", value = {
		"Delivered, null", "Delivered, ''", "Delivered, ' '", "delivered, null", "DeliveredIT, null", "DeliveredIT, ' '", "deliveredit, null", "' DeliveredIT ', null"
	})
	void testSerialNumberMissing(final String caseStatus, final String serialNumber) {
		when(contextMock.getDefaultConstraintMessageTemplate()).thenReturn(MESSAGE);
		when(contextMock.buildConstraintViolationWithTemplate(MESSAGE)).thenReturn(builderMock);
		when(builderMock.addPropertyNode(VALIDATED_NODE_SERIAL_NUMBER)).thenReturn(nodeBuilderMock);

		final var updateCaseRequest = UpdateCaseRequest.create()
			.withCaseStatus(caseStatus)
			.withSerialNumber(serialNumber)
			.withHardwareName("WB12345NY");

		assertThat(validator.isValid(updateCaseRequest, contextMock)).isFalse();

		verify(contextMock).disableDefaultConstraintViolation();
		verify(contextMock).getDefaultConstraintMessageTemplate();
		verify(contextMock).buildConstraintViolationWithTemplate(MESSAGE);
		verify(builderMock).addPropertyNode(VALIDATED_NODE_SERIAL_NUMBER);
		verify(nodeBuilderMock).addConstraintViolation();
		verifyNoMoreInteractions(contextMock, builderMock, nodeBuilderMock);
	}

	@ParameterizedTest
	@CsvSource(nullValues = "null", value = {
		"Delivered, null, null", "Delivered, '', null", "Delivered, null, ' '", "DELIVERED, null, null"
	})
	void testHardwareIdentifierMissing(final String caseStatus, final String hardwareName, final String imeiNumber) {
		when(contextMock.buildConstraintViolationWithTemplate(MESSAGE_HARDWARE_IDENTIFIER)).thenReturn(builderMock);
		when(builderMock.addPropertyNode(VALIDATED_NODE_HARDWARE_NAME)).thenReturn(nodeBuilderMock);
		when(builderMock.addPropertyNode(VALIDATED_NODE_IMEI_NUMBER)).thenReturn(nodeBuilderMock);

		final var updateCaseRequest = UpdateCaseRequest.create()
			.withCaseStatus(caseStatus)
			.withSerialNumber("FRGDZ1J")
			.withHardwareName(hardwareName)
			.withImeiNumber(imeiNumber);

		assertThat(validator.isValid(updateCaseRequest, contextMock)).isFalse();

		verify(contextMock).disableDefaultConstraintViolation();
		verify(contextMock, times(2)).buildConstraintViolationWithTemplate(MESSAGE_HARDWARE_IDENTIFIER);
		verify(builderMock).addPropertyNode(VALIDATED_NODE_HARDWARE_NAME);
		verify(builderMock).addPropertyNode(VALIDATED_NODE_IMEI_NUMBER);
		verify(nodeBuilderMock, times(2)).addConstraintViolation();
		verifyNoMoreInteractions(contextMock, builderMock, nodeBuilderMock);
	}

	@Test
	void testAllDeliveryIdentifiersMissing() {
		when(contextMock.getDefaultConstraintMessageTemplate()).thenReturn(MESSAGE);
		when(contextMock.buildConstraintViolationWithTemplate(MESSAGE)).thenReturn(builderMock);
		when(contextMock.buildConstraintViolationWithTemplate(MESSAGE_HARDWARE_IDENTIFIER)).thenReturn(builderMock);
		when(builderMock.addPropertyNode(VALIDATED_NODE_SERIAL_NUMBER)).thenReturn(nodeBuilderMock);
		when(builderMock.addPropertyNode(VALIDATED_NODE_HARDWARE_NAME)).thenReturn(nodeBuilderMock);
		when(builderMock.addPropertyNode(VALIDATED_NODE_IMEI_NUMBER)).thenReturn(nodeBuilderMock);

		assertThat(validator.isValid(UpdateCaseRequest.create().withCaseStatus("Delivered"), contextMock)).isFalse();

		verify(contextMock).disableDefaultConstraintViolation();
		verify(contextMock).getDefaultConstraintMessageTemplate();
		verify(contextMock).buildConstraintViolationWithTemplate(MESSAGE);
		verify(contextMock, times(2)).buildConstraintViolationWithTemplate(MESSAGE_HARDWARE_IDENTIFIER);
		verify(builderMock).addPropertyNode(VALIDATED_NODE_SERIAL_NUMBER);
		verify(builderMock).addPropertyNode(VALIDATED_NODE_HARDWARE_NAME);
		verify(builderMock).addPropertyNode(VALIDATED_NODE_IMEI_NUMBER);
		verify(nodeBuilderMock, times(3)).addConstraintViolation();
		verifyNoMoreInteractions(contextMock, builderMock, nodeBuilderMock);
	}
}
