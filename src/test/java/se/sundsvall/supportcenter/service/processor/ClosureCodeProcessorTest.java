package se.sundsvall.supportcenter.service.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.RESOLVED;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.CLOSURE_CODE_CHANGE_OF_HARDWARE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CASE_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CLOSURE_CODE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.STATUS_SOLVED;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Component;

import generated.client.pob.PobPayload;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.service.SupportCenterStatus;

@ExtendWith(MockitoExtension.class)
class ClosureCodeProcessorTest {

	@Mock
	private PobPayload pobPayloadMock;

	@Mock
	private Map<String, Object> mapMock;

	@InjectMocks
	private ClosureCodeProcessor processor;

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
		assertThat(processor.shouldProcess(UpdateCaseRequest.create().withCaseStatus(RESOLVED.getValue()))).isTrue();
		assertThat(processor.shouldProcess(null)).isFalse();
		Stream.of(SupportCenterStatus.values())
			.filter(status -> RESOLVED != status)
			.forEach(status -> assertThat(processor.shouldProcess(UpdateCaseRequest.create().withCaseStatus(status.getValue()))).isFalse());
	}

	@Test
	void preProcessWhenSerialNumberIsPresentAndPayloadStatusIsSolved() {

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var serialNumber = "serialNumber";
		final var request = UpdateCaseRequest.create().withSerialNumber(serialNumber);

		when(pobPayloadMock.getData()).thenReturn(mapMock);
		when(mapMock.get(KEY_CASE_STATUS)).thenReturn(STATUS_SOLVED);
		
		// Call
		processor.preProcess(pobKey, caseId, request, pobPayloadMock);

		// Verification
		verify(pobPayloadMock, times(2)).getData();
		verify(mapMock).get(KEY_CASE_STATUS);
		verify(mapMock).put(KEY_CLOSURE_CODE, CLOSURE_CODE_CHANGE_OF_HARDWARE);
	}

	@Test
	void preProcessWhenSerialNumberIsPresentButPayloadStatusIsNotSolved() {

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var serialNumber = "serialNumber";
		final var request = UpdateCaseRequest.create().withSerialNumber(serialNumber);

		when(pobPayloadMock.getData()).thenReturn(mapMock);
		
		// Call
		processor.preProcess(pobKey, caseId, request, pobPayloadMock);

		// Verification
		verify(pobPayloadMock).getData();
		verify(mapMock).get(KEY_CASE_STATUS);
		verify(mapMock, never()).put(any(), any());
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
		verify(pobPayloadMock, never()).getData();
		verify(mapMock, never()).get(any());
		verify(mapMock, never()).put(any(), any());
	}
	
	@Test
	void postProcess() {
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var request = UpdateCaseRequest.create();

		processor.postProcess(pobKey, caseId, request, pobPayloadMock);

		verifyNoInteractions(pobPayloadMock, mapMock);
		
	}
}
