package se.sundsvall.supportcenter.service.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.AWAITING_INFO;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Component;

import generated.client.pob.PobPayload;
import generated.client.pob.SuspensionInfo;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.integration.pob.POBClient;
import se.sundsvall.supportcenter.service.SupportCenterStatus;

@ExtendWith(MockitoExtension.class)
class SuspendCaseProcessorTest {

	@Mock
	private POBClient pobClientMock;
	
	@Mock
	private PobPayload pobPayloadMock;

	@InjectMocks
	private SuspendCaseProcessor processor;

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
		assertThat(processor.shouldProcess(UpdateCaseRequest.create().withCaseStatus(AWAITING_INFO.getValue()))).isTrue();
		assertThat(processor.shouldProcess(null)).isFalse();
		Stream.of(SupportCenterStatus.values())
			.filter(status -> AWAITING_INFO != status)
			.forEach(status -> assertThat(processor.shouldProcess(UpdateCaseRequest.create().withCaseStatus(status.getValue()))).isFalse());
	}

	@Test
	void preProcess() {

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var request = UpdateCaseRequest.create();

		// Call
		processor.preProcess(pobKey, caseId, request, pobPayloadMock);

		// Verification
		verify(pobClientMock).suspendCase(eq(pobKey), eq(caseId), any(SuspensionInfo.class));
		verifyNoInteractions(pobPayloadMock);
	}

	@Test
	void postProcess() {
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var request = UpdateCaseRequest.create();

		processor.postProcess(pobKey, caseId, request, pobPayloadMock);

		verifyNoInteractions(pobClientMock, pobPayloadMock);
	}
}
