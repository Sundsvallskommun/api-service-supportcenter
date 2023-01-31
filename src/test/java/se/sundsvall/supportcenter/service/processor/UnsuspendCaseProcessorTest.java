package se.sundsvall.supportcenter.service.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.OPEN;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_SUSPENSION;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Component;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

import generated.client.pob.PobPayload;
import se.sundsvall.dept44.exception.ClientProblem;
import se.sundsvall.dept44.exception.ServerProblem;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.integration.pob.POBClient;
import se.sundsvall.supportcenter.service.SupportCenterStatus;

@ExtendWith(MockitoExtension.class)
class UnsuspendCaseProcessorTest {

	private static final String POB_KEY = "pobKey";
	
	private static final String CASE_ID = "12345";
	
	private static final UpdateCaseRequest REQUEST = UpdateCaseRequest.create();

	@Mock
	private POBClient pobClientMock;
	
	@Mock
	private PobPayload pobPayloadMock;

	@Mock
	private Map<String, Object> mapMock;

	@InjectMocks
	private UnsuspendCaseProcessor processor;

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
		assertThat(processor.shouldProcess(UpdateCaseRequest.create().withCaseStatus(OPEN.getValue()))).isTrue();
		assertThat(processor.shouldProcess(null)).isFalse();
		Stream.of(SupportCenterStatus.values())
			.filter(status -> OPEN != status)
			.forEach(status -> assertThat(processor.shouldProcess(UpdateCaseRequest.create().withCaseStatus(status.getValue()))).isFalse());
	}

	@Test
	void preProcessWhenCaseIsSuspended() {
		// Mocks
		when(pobPayloadMock.getData()).thenReturn(mapMock);

		// Call
		processor.preProcess(POB_KEY, CASE_ID, REQUEST, pobPayloadMock);

		// Verification
		verify(pobClientMock).getSuspension(POB_KEY, CASE_ID);
		verify(pobClientMock).deleteSuspension(POB_KEY, CASE_ID);
		verify(pobPayloadMock).getData();
		verify(mapMock).put(KEY_SUSPENSION, null);
		
		verifyNoMoreInteractions(pobClientMock, pobPayloadMock, mapMock);
	}

	@Test
	void preProcessWhenCaseIsNotSuspended() {
		// Mocks
		when(pobClientMock.getSuspension(POB_KEY, CASE_ID)).thenThrow(new ClientProblem(Status.NOT_FOUND, "Not Found: Testexception"));
		
		// Call
		processor.preProcess(POB_KEY, CASE_ID, REQUEST, pobPayloadMock);

		// Verification
		verify(pobClientMock).getSuspension(POB_KEY, CASE_ID);
		verify(pobClientMock, never()).deleteSuspension(POB_KEY, CASE_ID);
		verify(pobPayloadMock, never()).getData();
		verify(mapMock, never()).put(KEY_SUSPENSION, null);
	}
	
	@Test
	void preProcessWhenNon404ExceptionIsThrown() {
		final var exception = new ServerProblem(Status.INTERNAL_SERVER_ERROR, "Internal Server Error: Testexception");
		
		// Mocks
		when(pobClientMock.getSuspension(POB_KEY, CASE_ID)).thenThrow(exception);
		
		// Call
		final var e = assertThrows(ThrowableProblem.class, () -> processor.preProcess(POB_KEY, CASE_ID, REQUEST, pobPayloadMock));
		
		// Verification
		assertThat(e).isSameAs(exception);
		verify(pobClientMock).getSuspension(POB_KEY, CASE_ID);
		verify(pobClientMock, never()).deleteSuspension(POB_KEY, CASE_ID);
		verify(pobPayloadMock, never()).getData();
		verify(mapMock, never()).put(KEY_SUSPENSION, null);
	}
	
	@Test
	void postProcess() {
		processor.postProcess(POB_KEY, CASE_ID, REQUEST, pobPayloadMock);

		verifyNoInteractions(pobClientMock, pobPayloadMock, mapMock);
	}
}
