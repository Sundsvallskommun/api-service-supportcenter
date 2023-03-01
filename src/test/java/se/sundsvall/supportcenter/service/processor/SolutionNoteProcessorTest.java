package se.sundsvall.supportcenter.service.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportcenter.api.model.enums.NoteType.SOLUTION;
import static se.sundsvall.supportcenter.api.model.enums.NoteType.WORKNOTE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CASE_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.STATUS_CLOSED;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.STATUS_SOLVED;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Component;

import generated.client.pob.PobMemo;
import generated.client.pob.PobPayload;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.service.SupportCenterStatus;

@ExtendWith(MockitoExtension.class)
class SolutionNoteProcessorTest {

	@Captor
	private ArgumentCaptor<Map<String, PobMemo>> argumentCaptor;

	@Mock
	private PobPayload pobPayloadMock;

	@Mock
	private Map<String, Object> dataMapMock;

	@InjectMocks
	private SolutionNoteProcessor processor;

	@Test
	void isAssignableFrom() {
		assertThat(ProcessorInterface.class).isAssignableFrom(processor.getClass());
	}
	
	@Test
	void hasComponentAnnotation() {
		assertThat(processor.getClass().getAnnotation(Component.class)).isNotNull();
	}

	@ParameterizedTest
	@EnumSource(value = SupportCenterStatus.class)
	void shouldProcessForAllStatuses(SupportCenterStatus status) {
		assertThat(processor.shouldProcess(UpdateCaseRequest.create().withCaseStatus(status.getValue()))).isTrue();
	}

	@Test
	void shouldNotProcessForNull() {
		assertThat(processor.shouldProcess(null)).isFalse();
	}

	@Test
	void preProcessWhenNoteContainsSolutionAndPayloadStatusIsSolved() {

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var request = UpdateCaseRequest.create();
		final var memoMap = new HashMap<String, PobMemo>();

		memoMap.put(SOLUTION.toValue(), new PobMemo());
		memoMap.put(WORKNOTE.toValue(), new PobMemo());

		// Setup mocking
		when(pobPayloadMock.getData()).thenReturn(dataMapMock);
		when(dataMapMock.get(KEY_CASE_STATUS)).thenReturn(STATUS_SOLVED);
		when(pobPayloadMock.getMemo()).thenReturn(memoMap);
		
		// Call
		processor.preProcess(pobKey, caseId, request, pobPayloadMock);

		// Verification
		verify(pobPayloadMock, times(2)).getData();
		verify(pobPayloadMock, times(2)).getMemo();
		verify(dataMapMock).get(KEY_CASE_STATUS);
		verify(pobPayloadMock, never()).setMemo(any());
	}

	@Test
	void preProcessWhenNoteNotEqualsSolutionAndPayloadStatusEqualsSolved() {

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var request = UpdateCaseRequest.create();
		final var memoMap = new HashMap<String, PobMemo>();

		memoMap.put(WORKNOTE.toValue(), new PobMemo());

		// Setup mocking
		when(pobPayloadMock.getData()).thenReturn(dataMapMock);
		when(pobPayloadMock.getMemo()).thenReturn(memoMap);

		// Call
		processor.preProcess(pobKey, caseId, request, pobPayloadMock);

		// Verification
		verify(pobPayloadMock).getData();
		verify(pobPayloadMock, times(2)).getMemo();
		verify(pobPayloadMock, never()).setMemo(any());
	}

	@Test
	void preProcessWhenNoteOnlyContainsSolutionButPayloadStatusNotEqualsSolved() {

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var request = UpdateCaseRequest.create();
		final var memoMap = new HashMap<String, PobMemo>();

		memoMap.put(SOLUTION.toValue(), new PobMemo());

		// Setup mocking
		doCallRealMethod().when(pobPayloadMock).setMemo(any());
		when(pobPayloadMock.getData()).thenReturn(dataMapMock);
		when(dataMapMock.get(KEY_CASE_STATUS)).thenReturn(STATUS_CLOSED);
		when(pobPayloadMock.getMemo()).thenReturn(memoMap);

		// Call
		processor.preProcess(pobKey, caseId, request, pobPayloadMock);

		// Verification
		verify(pobPayloadMock, times(3)).getData();
		verify(pobPayloadMock, times(3)).getMemo();
		verify(dataMapMock, times(2)).get(KEY_CASE_STATUS);
		verify(pobPayloadMock).setMemo(isNull());
	}

	@Test
	void preProcessWhenNoteContainsSolutionButPayloadStatusNotEqualsSolved() {

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var request = UpdateCaseRequest.create();
		final var memoMap = new HashMap<String, PobMemo>();

		memoMap.put(WORKNOTE.toValue(), new PobMemo());
		memoMap.put(SOLUTION.toValue(), new PobMemo());

		// Setup mocking
		doCallRealMethod().when(pobPayloadMock).setMemo(any());
		when(pobPayloadMock.getData()).thenReturn(dataMapMock);
		when(dataMapMock.get(KEY_CASE_STATUS)).thenReturn(STATUS_CLOSED);
		when(pobPayloadMock.getMemo()).thenReturn(memoMap);

		// Call
		processor.preProcess(pobKey, caseId, request, pobPayloadMock);

		// Verifications and assertions
		verify(pobPayloadMock, times(3)).getData();
		verify(pobPayloadMock, times(3)).getMemo();
		verify(dataMapMock, times(2)).get(KEY_CASE_STATUS);
		verify(pobPayloadMock).setMemo(argumentCaptor.capture());

		assertThat(argumentCaptor.getValue()).hasSize(1).containsKey(WORKNOTE.toValue());
	}

	@Test
	void preProcessWhenNoteNotEqualsSolutionAndPayloadStatusNotEqualsSolved() {

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var request = UpdateCaseRequest.create();
		final var memoMap = new HashMap<String, PobMemo>();

		memoMap.put(WORKNOTE.toValue(), new PobMemo());

		// Setup mocking
		when(pobPayloadMock.getData()).thenReturn(dataMapMock);
		when(pobPayloadMock.getMemo()).thenReturn(memoMap);

		// Call
		processor.preProcess(pobKey, caseId, request, pobPayloadMock);

		// Verification
		verify(pobPayloadMock).getData();
		verify(pobPayloadMock, times(2)).getMemo();
		verify(pobPayloadMock, never()).setMemo(any());
	}
	
	@Test
	void postProcess() {
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var request = UpdateCaseRequest.create();

		processor.postProcess(pobKey, caseId, request, pobPayloadMock);

		verifyNoInteractions(pobPayloadMock, dataMapMock);
		
	}
}
