package se.sundsvall.supportcenter.service.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CI_INFO;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CI_INFO2;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Component;

import generated.client.pob.PobPayload;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.integration.pob.POBClient;
import se.sundsvall.supportcenter.service.ConfigurationService;

@ExtendWith(MockitoExtension.class)
class SerialNumberProcessorTest {

	@Mock
	private POBClient pobClientMock;

	@Mock
	private ConfigurationService configurationServiceMock;

	@Mock
	private PobPayload pobPayloadMock;

	@Mock
	private Map<String, Object> mapMock;

	@InjectMocks
	private SerialNumberProcessor processor;

	@Test
	void isAssignableFrom() {
		assertThat(ProcessorInterface.class).isAssignableFrom(processor.getClass());
	}
	
	@Test
	void hasComponentAnnotation() {
		assertThat(processor.getClass().getAnnotation(Component.class)).isNotNull();
	}

	@Test
	void shouldProcess() {
		assertThat(processor.shouldProcess(UpdateCaseRequest.create())).isTrue();
		assertThat(processor.shouldProcess(null)).isFalse();
	}
	
	@Test
	void preProcessWhenSerialNumberDiffersFromExisting() {

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var serialNumber = "serialNumber";
		final var serialNumberId = "123456789";
		final var request = UpdateCaseRequest.create().withSerialNumber(serialNumber);

		// Set up existingCase to match this jsonPath: $['Data']['CIInfo.Ci']['Data']['SerialNumber']
		when(pobClientMock.getCase(pobKey, caseId)).thenReturn(new PobPayload().data(Map.of("CIInfo.Ci", Map.of("Data", Map.of("SerialNumber", "xxx-111")))));
		when(configurationServiceMock.getSerialNumberId(pobKey, serialNumber)).thenReturn(serialNumberId);
		when(pobPayloadMock.getData()).thenReturn(mapMock);
		
		// Call
		processor.preProcess(pobKey, caseId, request, pobPayloadMock);

		// Verification
		verify(configurationServiceMock, never()).validateCaseCategory(any(), any());
		verify(configurationServiceMock, never()).validateClosureCode(any(), any());
		verify(configurationServiceMock).getSerialNumberId(pobKey, serialNumber);
		verify(pobClientMock).getCase(pobKey, caseId);
		verify(pobPayloadMock).getData();
		verify(mapMock).put(KEY_CI_INFO2, serialNumberId);
		verifyNoMoreInteractions(configurationServiceMock, pobClientMock, pobPayloadMock, mapMock);
	}

	@Test
	void preProcessWhenSerialNumberIsIdenticalToExistingSerialNumber() {

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var serialNumber = "serialNumber";
		final var serialNumberId = "123456789";
		final var request = UpdateCaseRequest.create().withSerialNumber(serialNumber);

		// Set up existingCase to match this jsonPath: $['Data']['CIInfo.Ci']['Data']['SerialNumber']
		when(pobClientMock.getCase(pobKey, caseId)).thenReturn(new PobPayload().data(Map.of("CIInfo.Ci", Map.of("Data", Map.of("SerialNumber", serialNumber)))));
		when(configurationServiceMock.getSerialNumberId(pobKey, serialNumber)).thenReturn(serialNumberId);
		when(pobPayloadMock.getData()).thenReturn(mapMock);

		// Call
		processor.preProcess(pobKey, caseId, request, pobPayloadMock);

		// Verification
		verify(configurationServiceMock).getSerialNumberId(pobKey, serialNumber);
		verify(pobClientMock).getCase(pobKey, caseId);
		verify(pobPayloadMock).getData();
		verify(mapMock).put(KEY_CI_INFO, serialNumberId);
		verifyNoMoreInteractions(configurationServiceMock, pobClientMock, pobPayloadMock, mapMock);
	}

	@Test
	void preProcessWithSerialNumberWhenThereIsNoExistingSerialNumber() {

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var serialNumber = "serialNumber";
		final var serialNumberId = "123456789";
		final var request = UpdateCaseRequest.create().withSerialNumber(serialNumber);

		when(pobClientMock.getCase(pobKey, caseId)).thenReturn(new PobPayload()); // I.e. no existing "CIInfo.Ci"
		when(configurationServiceMock.getSerialNumberId(pobKey, serialNumber)).thenReturn(serialNumberId);
		when(pobPayloadMock.getData()).thenReturn(mapMock);

		// Call
		processor.preProcess(pobKey, caseId, request, pobPayloadMock);

		// Verification
		verify(configurationServiceMock).getSerialNumberId(pobKey, serialNumber);
		verify(pobClientMock).getCase(pobKey, caseId);
		verify(pobPayloadMock).getData();
		verify(mapMock).put(KEY_CI_INFO, serialNumberId);
		verifyNoMoreInteractions(configurationServiceMock, pobClientMock, pobPayloadMock, mapMock);
	}

	@Test
	void preProcessWhenBlankSerialNumber() {

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var serialNumber = " ";
		final var request = UpdateCaseRequest.create().withSerialNumber(serialNumber);

		// Call
		processor.preProcess(pobKey, caseId, request, pobPayloadMock);

		// Verification
		verify(configurationServiceMock, never()).getSerialNumberId(any(), any());
		verify(pobClientMock, never()).getCase(any(), any());
		verifyNoMoreInteractions(configurationServiceMock, pobClientMock, pobPayloadMock, mapMock);
	}

	@Test
	void postProcess() {
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var request = UpdateCaseRequest.create();

		processor.postProcess(pobKey, caseId, request, pobPayloadMock);

		verifyNoInteractions(configurationServiceMock, pobClientMock, pobPayloadMock, mapMock);
	}

}
