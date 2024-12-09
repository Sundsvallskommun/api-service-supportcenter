package se.sundsvall.supportcenter.service.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.DELIVERED;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CASE_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.STATUS_SOLVED;

import generated.client.pob.PobMemo;
import generated.client.pob.PobPayload;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Component;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.service.SupportCenterStatus;

@ExtendWith(MockitoExtension.class)
class ExtraWorkNoteProcessorTest {

	@Mock
	private PobPayload pobPayloadMock;

	@Mock
	private Map<String, PobMemo> pobMemoMapMock;

	@Mock
	private Map<String, Object> mapMock;

	@InjectMocks
	private ExtraWorknoteProcessor processor;

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
		assertThat(processor.shouldProcess(UpdateCaseRequest.create().withCaseStatus(DELIVERED.getValue()))).isTrue();
		assertThat(processor.shouldProcess(null)).isFalse();
		Stream.of(SupportCenterStatus.values())
			.filter(status -> DELIVERED != status)
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
		when(pobPayloadMock.getMemo()).thenReturn(pobMemoMapMock);

		// Call
		processor.preProcess(pobKey, caseId, request, pobPayloadMock);

		// Verification
		verify(pobPayloadMock).getData();
		verify(mapMock).get(KEY_CASE_STATUS);
		verify(pobPayloadMock).getMemo();
		verify(pobMemoMapMock).putAll(any());
	}

	@Test
	void preProcessWhenPayloadStatusIsSolved() {

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var request = UpdateCaseRequest.create();

		when(pobPayloadMock.getData()).thenReturn(mapMock);
		when(mapMock.get(KEY_CASE_STATUS)).thenReturn(STATUS_SOLVED);
		when(pobPayloadMock.getMemo()).thenReturn(pobMemoMapMock);

		// Call
		processor.preProcess(pobKey, caseId, request, pobPayloadMock);

		// Verification
		verify(pobPayloadMock).getData();
		verify(mapMock).get(KEY_CASE_STATUS);
		verify(pobPayloadMock).getMemo();
		verify(pobMemoMapMock).putAll(any());
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
	void postProcess() {
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var request = UpdateCaseRequest.create();

		processor.postProcess(pobKey, caseId, request, pobPayloadMock);

		verifyNoInteractions(pobPayloadMock, mapMock);
	}
}
