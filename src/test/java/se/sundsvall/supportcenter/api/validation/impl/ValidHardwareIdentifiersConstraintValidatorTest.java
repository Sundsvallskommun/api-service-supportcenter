package se.sundsvall.supportcenter.api.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
class ValidHardwareIdentifiersConstraintValidatorTest {

	private static final String MESSAGE = "hardwareName and imeiNumber must not be provided at the same time";
	private static final String VALIDATED_NODE_HARDWARE_NAME = "hardwareName";
	private static final String VALIDATED_NODE_IMEI_NUMBER = "imeiNumber";

	@Mock
	private ConstraintValidatorContext contextMock;

	@Mock
	private ConstraintViolationBuilder builderMock;

	@Mock
	private NodeBuilderCustomizableContext nodeBuilderMock;

	private final ValidHardwareIdentifiersConstraintValidator validator = new ValidHardwareIdentifiersConstraintValidator();

	@Test
	void testNullRequest() {
		assertThat(validator.isValid(null, contextMock)).isTrue();

		verifyNoInteractions(contextMock, builderMock, nodeBuilderMock);
	}

	@Test
	void testNoHardwareIdentifiers() {
		assertThat(validator.isValid(UpdateCaseRequest.create(), contextMock)).isTrue();

		verifyNoInteractions(contextMock, builderMock, nodeBuilderMock);
	}

	@Test
	void testOnlyHardwareName() {
		final var updateCaseRequest = UpdateCaseRequest.create().withHardwareName("WB12345NY");

		assertThat(validator.isValid(updateCaseRequest, contextMock)).isTrue();

		verifyNoInteractions(contextMock, builderMock, nodeBuilderMock);
	}

	@Test
	void testOnlyImeiNumber() {
		final var updateCaseRequest = UpdateCaseRequest.create().withImeiNumber("702548572062338");

		assertThat(validator.isValid(updateCaseRequest, contextMock)).isTrue();

		verifyNoInteractions(contextMock, builderMock, nodeBuilderMock);
	}

	/**
	 * A blank value counts as provided, since an empty string sent in either field overwrites the other one in POB just as
	 * effectively as a non-blank value does.
	 */
	@ParameterizedTest
	@CsvSource({
		"WB12345NY, 702548572062338", "WB12345NY, ''", "'', 702548572062338", "'', ''"
	})
	void testBothHardwareNameAndImeiNumber(final String hardwareName, final String imeiNumber) {
		when(contextMock.getDefaultConstraintMessageTemplate()).thenReturn(MESSAGE);
		when(contextMock.buildConstraintViolationWithTemplate(MESSAGE)).thenReturn(builderMock);
		when(builderMock.addPropertyNode(VALIDATED_NODE_HARDWARE_NAME)).thenReturn(nodeBuilderMock);
		when(builderMock.addPropertyNode(VALIDATED_NODE_IMEI_NUMBER)).thenReturn(nodeBuilderMock);

		final var updateCaseRequest = UpdateCaseRequest.create()
			.withHardwareName(hardwareName)
			.withImeiNumber(imeiNumber);

		assertThat(validator.isValid(updateCaseRequest, contextMock)).isFalse();

		verify(contextMock).disableDefaultConstraintViolation();
		verify(contextMock).getDefaultConstraintMessageTemplate();
		verify(contextMock, times(2)).buildConstraintViolationWithTemplate(MESSAGE);
		verify(builderMock).addPropertyNode(VALIDATED_NODE_HARDWARE_NAME);
		verify(builderMock).addPropertyNode(VALIDATED_NODE_IMEI_NUMBER);
		verify(nodeBuilderMock, times(2)).addConstraintViolation();
		verifyNoMoreInteractions(contextMock, builderMock, nodeBuilderMock);
	}
}
