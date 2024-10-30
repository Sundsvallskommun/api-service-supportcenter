package se.sundsvall.supportcenter.service.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.ASSIGN_BACK;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CASE_STATUS;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Component;

import generated.client.pob.PobPayload;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.service.SupportCenterStatus;

@ExtendWith(MockitoExtension.class)
class CaseStatusProcessorTest {

	@Mock
	private PobPayload pobPayloadMock;

	@Mock
	private Map<String, Object> mapMock;

	@InjectMocks
	private CaseStatusProcessor processor;

	@Test
	void isAssignableFrom() {
		assertThat(ProcessorInterface.class).isAssignableFrom(processor.getClass());
	}

	@Test
	void hasComponentAnnotation() {
		assertThat(processor.getClass().getAnnotation(Component.class)).isNotNull();
	}

	@ParameterizedTest
	@EnumSource(value = SupportCenterStatus.class, mode = Mode.EXCLUDE, names = "ASSIGN_BACK")
	@NullSource
	void shouldNotProcess(SupportCenterStatus status) {
		final var statusAsString = Optional.ofNullable(status).map(SupportCenterStatus::getValue).orElse(null);
		assertThat(processor.shouldProcess(UpdateCaseRequest.create().withCaseStatus(statusAsString))).isFalse();
	}

	@Test
	void shouldProcess() {
		assertThat(processor.shouldProcess(UpdateCaseRequest.create().withCaseStatus(ASSIGN_BACK.getValue()))).isTrue();
	}

	@Test
	void preProcess() {

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var request = UpdateCaseRequest.create();

		when(pobPayloadMock.getData()).thenReturn(mapMock);

		// Call
		processor.preProcess(pobKey, caseId, request, pobPayloadMock);

		// Verification
		verify(pobPayloadMock, times(2)).getData();
		verify(mapMock).remove(KEY_CASE_STATUS);
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
