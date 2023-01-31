package se.sundsvall.supportcenter.service;

import generated.client.pob.PobPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.supportcenter.api.model.Note;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.api.model.enums.NoteType;
import se.sundsvall.supportcenter.integration.pob.POBClient;
import se.sundsvall.supportcenter.service.processor.ProcessorInterface;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.CUSTOM_STATUS_MAP;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.DEFAULT_TYPE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CASE_CATEGORY;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CASE_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CLOSURE_CODE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_EXTERNAL_CASE_ID;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_ID;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_RESPONSIBLE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_RESPONSIBLE_GROUP;

@ExtendWith(MockitoExtension.class)
class UpdateCaseServiceTest {

	@Mock
	private POBClient pobClientMock;

	@Mock
	private ConfigurationService configurationServiceMock;

	@Mock
	private TrueProcessor trueProcessorMock;

	@Mock
	private FalseProcessor falseProcessorMock;
	
	@Spy
	private List<ProcessorInterface> processorListSpy = new ArrayList<>();
	
	@InjectMocks
	private CaseService caseService;

	@Captor
	private ArgumentCaptor<PobPayload> pobPayloadCaptor;

	@BeforeEach
	void setup() {
		processorListSpy.clear();
		processorListSpy.add(trueProcessorMock);
		processorListSpy.add(falseProcessorMock);
	}
	
	@Test
	void updateCase() {
		when(trueProcessorMock.shouldProcess(any())).thenReturn(true);

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var externalCaseId = "External-12345";
		final var noteText = "A minor note";
		final var responsibleGroup = "responsibleGroup";
		final var caseCategory = "caseCategory";
		final var caseStatus = "In Process";
		final var closureCode = "closureCode";
		final var noteType = NoteType.WORKNOTE;
		final var request = UpdateCaseRequest.create()
			.withCaseCategory(caseCategory)
			.withCaseStatus(caseStatus)
			.withClosureCode(closureCode)
			.withExternalCaseId(externalCaseId)
			.withNote(Note.create()
				.withText(noteText)
				.withType(noteType))
			.withResponsibleGroup(responsibleGroup);

		// Call
		caseService.updateCase(pobKey, caseId, request);

		// Verification
		verify(configurationServiceMock).validateCaseCategory(pobKey, caseCategory);
		verify(configurationServiceMock).validateClosureCode(pobKey, closureCode);
		verify(pobClientMock).updateCase(eq(pobKey), pobPayloadCaptor.capture());
		processorListSpy.forEach(processor -> verify(processor, times(2)).shouldProcess(request));
		verify(falseProcessorMock, never()).preProcess(any(), any(), any(), any());
		verify(falseProcessorMock, never()).postProcess(any(), any(), any(), any());
		verify(trueProcessorMock).preProcess(eq(pobKey), eq(caseId), eq(request), any(PobPayload.class));
		verify(trueProcessorMock).postProcess(eq(pobKey), eq(caseId), eq(request), any(PobPayload.class));
		verifyNoMoreInteractions(pobClientMock);

		final var pobPayloadValue = pobPayloadCaptor.getValue();
		assertThat(pobPayloadValue).isNotNull();
		assertThat(pobPayloadValue.getType()).isEqualTo(DEFAULT_TYPE);
		assertThat(pobPayloadValue.getData()).isNotNull()
			.containsEntry(KEY_ID, caseId)
			.containsEntry(KEY_CASE_CATEGORY, caseCategory)
			.containsEntry(KEY_CASE_STATUS, caseStatus)
			.containsEntry(KEY_CLOSURE_CODE, closureCode)
			.containsEntry(KEY_EXTERNAL_CASE_ID, externalCaseId)
			.containsEntry(KEY_RESPONSIBLE_GROUP, responsibleGroup)
			.containsEntry(KEY_RESPONSIBLE, null);
		assertThat(pobPayloadValue.getMemo()).isNotNull();
		assertThat(pobPayloadValue.getMemo().get(NoteType.WORKNOTE.toValue()).getMemo()).isEqualTo(noteText + " | Status: 'In Process'");
	}

	@ParameterizedTest
	@ValueSource(strings = { "Awaiting info", "Open", "Cancelled" })
	void updateCaseForKnownCubeStatuses(String incomingCaseStatus) {
		when(trueProcessorMock.shouldProcess(any())).thenReturn(true);
		
		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var externalCaseId = "External-12345";
		final var noteText = "A minor note";
		final var responsibleGroup = "responsibleGroup";
		final var caseCategory = "caseCategory";
		final var closureCode = "closureCode";
		final var noteType = NoteType.SOLUTION;
		final var request = UpdateCaseRequest.create()
			.withCaseCategory(caseCategory)
			.withCaseStatus(incomingCaseStatus)
			.withClosureCode(closureCode)
			.withExternalCaseId(externalCaseId)
			.withNote(Note.create()
				.withText(noteText)
				.withType(noteType))
			.withResponsibleGroup(responsibleGroup);

		// Call
		caseService.updateCase(pobKey, caseId, request);

		// Verification
		verify(configurationServiceMock).validateCaseCategory(pobKey, caseCategory);
		verify(configurationServiceMock).validateClosureCode(pobKey, closureCode);
		
		if (!incomingCaseStatus.equals(SupportCenterStatus.AWAITING_INFO.getValue())) {
			verify(pobClientMock, times(incomingCaseStatus.equals(SupportCenterStatus.RESOLVED.getValue()) ? 2 : 1)).updateCase(eq(pobKey), pobPayloadCaptor.capture());
			
			final var pobPayloadValue = pobPayloadCaptor.getValue();
			assertThat(pobPayloadValue).isNotNull();
			assertThat(pobPayloadValue.getType()).isEqualTo(DEFAULT_TYPE);
			assertThat(pobPayloadValue.getData()).isNotNull()
				.containsEntry(KEY_ID, caseId)
				.containsEntry(KEY_CASE_CATEGORY, caseCategory)
				.containsEntry(KEY_CASE_STATUS, CUSTOM_STATUS_MAP.get(incomingCaseStatus).get(0).getAttributes().get(KEY_CASE_STATUS))
				.containsEntry(KEY_CLOSURE_CODE, closureCode)
				.containsEntry(KEY_EXTERNAL_CASE_ID, incomingCaseStatus.equals(SupportCenterStatus.CANCELLED.getValue()) ? null : externalCaseId)
				.containsEntry(KEY_RESPONSIBLE_GROUP, incomingCaseStatus.equals(SupportCenterStatus.CANCELLED.getValue()) ? "Second Line IT" :responsibleGroup)
				.containsEntry(KEY_RESPONSIBLE, null);
			assertThat(pobPayloadValue.getMemo()).isNotNull();
			assertThat(pobPayloadValue.getMemo().get(CUSTOM_STATUS_MAP.get(incomingCaseStatus).get(0).getStatusNoteType().toValue()).getMemo()).isEqualTo("Status: '" + incomingCaseStatus + "'");
			assertThat(pobPayloadValue.getMemo().get(NoteType.SOLUTION.toValue()).getMemo()).isEqualTo(noteText);
		}
		
		processorListSpy.forEach(processor -> verify(processor, times(2)).shouldProcess(request));
		verify(falseProcessorMock, never()).preProcess(any(), any(), any(), any());
		verify(falseProcessorMock, never()).postProcess(any(), any(), any(), any());
		verify(trueProcessorMock).preProcess(eq(pobKey), eq(caseId), eq(request), any(PobPayload.class));
		verify(trueProcessorMock).postProcess(eq(pobKey), eq(caseId), eq(request), any(PobPayload.class));
		verifyNoMoreInteractions(pobClientMock);
	}

	@Test
	void updateCaseForCubeStatusResolvedWillTriggerTwoPOBCalls() {
		when(trueProcessorMock.shouldProcess(any())).thenReturn(true);

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var noteText = "Unit replaced";
		final var caseStatus = "Resolved";
		final var noteType = NoteType.SOLUTION;
		final var serialNumber = "serialNumber";
		final var externalCaseId = "externalCaseID";
		final var request = UpdateCaseRequest.create()
			.withCaseStatus(caseStatus)
			.withNote(Note.create()
				.withText(noteText)
				.withType(noteType))
			.withExternalCaseId(externalCaseId)
			.withSerialNumber(serialNumber);

		// Call (this will trigger two calls to POB)
		caseService.updateCase(pobKey, caseId, request);

		// Verification
		verify(configurationServiceMock, never()).validateCaseCategory(any(), any());
		verify(pobClientMock, times(2)).updateCase(eq(pobKey), pobPayloadCaptor.capture());
		processorListSpy.forEach(processor -> verify(processor, times(4)).shouldProcess(request));
		verify(falseProcessorMock, never()).preProcess(any(), any(), any(), any());
		verify(falseProcessorMock, never()).postProcess(any(), any(), any(), any());
		verify(trueProcessorMock, times(2)).preProcess(eq(pobKey), eq(caseId), eq(request), any(PobPayload.class));
		verify(trueProcessorMock, times(2)).postProcess(eq(pobKey), eq(caseId), eq(request), any(PobPayload.class));
		verifyNoMoreInteractions(pobClientMock);

		final var pobPayloadValues = pobPayloadCaptor.getAllValues();
		assertThat(pobPayloadValues).hasSize(2);

		// Assert first call.
		assertThat(pobPayloadValues.get(0).getType()).isEqualTo(DEFAULT_TYPE);
		assertThat(pobPayloadValues.get(0).getData()).isNotNull()
			.containsEntry(KEY_ID, caseId)
			.containsEntry(KEY_CASE_STATUS, CUSTOM_STATUS_MAP.get(caseStatus).get(0).getAttributes().get(KEY_CASE_STATUS))
			.containsEntry(KEY_CLOSURE_CODE, CUSTOM_STATUS_MAP.get(caseStatus).get(0).getAttributes().get(KEY_CLOSURE_CODE)) // First call contain closure code.
			.containsEntry(KEY_EXTERNAL_CASE_ID, ""); // First call should contain an empty string for external case id.
		assertThat(pobPayloadValues.get(0).getMemo()).hasSize(2);
		assertThat(pobPayloadValues.get(0).getMemo().get(CUSTOM_STATUS_MAP.get(caseStatus).get(0).getStatusNoteType().toValue()).getMemo()).isEqualTo("Status: '" + caseStatus + "'");
		assertThat(pobPayloadValues.get(0).getMemo().get(NoteType.SOLUTION.toValue()).getMemo()).isEqualTo(noteText);

		// Assert second call.
		assertThat(pobPayloadValues.get(1).getType()).isEqualTo(DEFAULT_TYPE);
		assertThat(pobPayloadValues.get(1).getData()).isNotNull()
			.containsEntry(KEY_ID, caseId)
			.containsEntry(KEY_CASE_STATUS, CUSTOM_STATUS_MAP.get(caseStatus).get(1).getAttributes().get(KEY_CASE_STATUS))
			.containsEntry(KEY_EXTERNAL_CASE_ID, "") // Second call shouldn't contain any value of external case id.
			.doesNotContainKey(KEY_CLOSURE_CODE); // Second call shouldn't contain closure code.
		assertThat(pobPayloadValues.get(1).getMemo()).hasSize(1);
		assertThat(pobPayloadValues.get(1).getMemo().get(NoteType.SOLUTION.toValue()).getMemo()).isEqualTo(noteText);
	}

	@ParameterizedTest
	@ValueSource(strings = { "Processed", "Reserved", "Picking", "Despatched" })
	void updateCaseForKnownNetstatStatuses(String incomingCaseStatus) {
		when(trueProcessorMock.shouldProcess(any())).thenReturn(true);

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var externalCaseId = "External-12345";
		final var noteText = "A minor note";
		final var responsibleGroup = "responsibleGroup";
		final var caseCategory = "caseCategory";
		final var closureCode = "closureCode";
		final var noteType = NoteType.SOLUTION;
		final var request = UpdateCaseRequest.create()
			.withCaseCategory(caseCategory)
			.withCaseStatus(incomingCaseStatus)
			.withClosureCode(closureCode)
			.withExternalCaseId(externalCaseId)
			.withNote(Note.create()
				.withText(noteText)
				.withType(noteType))
			.withResponsibleGroup(responsibleGroup);

		// Call
		caseService.updateCase(pobKey, caseId, request);

		// Verification
		verify(configurationServiceMock).validateCaseCategory(pobKey, caseCategory);
		verify(configurationServiceMock).validateClosureCode(pobKey, closureCode);
		verify(pobClientMock).updateCase(eq(pobKey), pobPayloadCaptor.capture());
		processorListSpy.forEach(processor -> verify(processor, times(2)).shouldProcess(request));
		verify(falseProcessorMock, never()).preProcess(any(), any(), any(), any());
		verify(falseProcessorMock, never()).postProcess(any(), any(), any(), any());
		verify(trueProcessorMock).preProcess(eq(pobKey), eq(caseId), eq(request), any(PobPayload.class));
		verify(trueProcessorMock).postProcess(eq(pobKey), eq(caseId), eq(request), any(PobPayload.class));
		verifyNoMoreInteractions(pobClientMock);

		final var pobPayloadValue = pobPayloadCaptor.getValue();
		assertThat(pobPayloadValue).isNotNull();
		assertThat(pobPayloadValue.getType()).isEqualTo(DEFAULT_TYPE);
		assertThat(pobPayloadValue.getData()).isNotNull()
			.containsEntry(KEY_ID, caseId)
			.containsEntry(KEY_CASE_CATEGORY, caseCategory)
			.containsEntry(KEY_CASE_STATUS, CUSTOM_STATUS_MAP.get(incomingCaseStatus).get(0).getAttributes().get(KEY_CASE_STATUS))
			.containsEntry(KEY_CLOSURE_CODE, closureCode)
			.containsEntry(KEY_EXTERNAL_CASE_ID, externalCaseId)
			.containsEntry(KEY_RESPONSIBLE_GROUP, responsibleGroup)
			.containsEntry(KEY_RESPONSIBLE, null);
		assertThat(pobPayloadValue.getMemo()).isNotNull();
		assertThat(pobPayloadValue.getMemo().get(CUSTOM_STATUS_MAP.get(incomingCaseStatus).get(0).getStatusNoteType().toValue()).getMemo()).isEqualTo("Status: '" + incomingCaseStatus + "'");
		assertThat(pobPayloadValue.getMemo().get(NoteType.SOLUTION.toValue()).getMemo()).isEqualTo(noteText);
	}

	@Test
	void updateCaseForUnknownStatus() {
		when(trueProcessorMock.shouldProcess(any())).thenReturn(true);

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var externalCaseId = "External-12345";
		final var noteText = "A minor note";
		final var responsibleGroup = "responsibleGroup";
		final var caseCategory = "caseCategory";
		final var closureCode = "closureCode";
		final var caseStatus = "This is an unknown status";
		final var noteType = NoteType.PROBLEM;
		final var request = UpdateCaseRequest.create()
			.withCaseCategory(caseCategory)
			.withCaseStatus(caseStatus)
			.withClosureCode(closureCode)
			.withExternalCaseId(externalCaseId)
			.withNote(Note.create()
				.withText(noteText)
				.withType(noteType))
			.withResponsibleGroup(responsibleGroup);

		// Call
		caseService.updateCase(pobKey, caseId, request);

		// Verification
		verify(configurationServiceMock).validateCaseCategory(pobKey, caseCategory);
		verify(configurationServiceMock).validateClosureCode(pobKey, closureCode);
		verify(pobClientMock).updateCase(eq(pobKey), pobPayloadCaptor.capture());
		processorListSpy.forEach(processor -> verify(processor, times(2)).shouldProcess(request));
		verify(falseProcessorMock, never()).preProcess(any(), any(), any(), any());
		verify(falseProcessorMock, never()).postProcess(any(), any(), any(), any());
		verify(trueProcessorMock).preProcess(eq(pobKey), eq(caseId), eq(request), any(PobPayload.class));
		verify(trueProcessorMock).postProcess(eq(pobKey), eq(caseId), eq(request), any(PobPayload.class));
		verifyNoMoreInteractions(pobClientMock);

		final var pobPayloadValue = pobPayloadCaptor.getValue();
		assertThat(pobPayloadValue).isNotNull();
		assertThat(pobPayloadValue.getType()).isEqualTo(DEFAULT_TYPE);
		assertThat(pobPayloadValue.getData()).isNotNull()
			.containsEntry(KEY_ID, caseId)
			.containsEntry(KEY_CASE_CATEGORY, caseCategory)
			.containsEntry(KEY_CASE_STATUS, caseStatus)
			.containsEntry(KEY_CLOSURE_CODE, closureCode)
			.containsEntry(KEY_EXTERNAL_CASE_ID, externalCaseId)
			.containsEntry(KEY_RESPONSIBLE_GROUP, responsibleGroup)
			.containsEntry(KEY_RESPONSIBLE, null);
		assertThat(pobPayloadValue.getMemo()).isNotNull();
		assertThat(pobPayloadValue.getMemo().get(NoteType.WORKNOTE.toValue()).getMemo()).isEqualTo("Status: '" + caseStatus + "'");
		assertThat(pobPayloadValue.getMemo().get(NoteType.PROBLEM.toValue()).getMemo()).isEqualTo(noteText);
	}

	@Test
	void updateCaseForNetstatStatusDeliveredWillTriggerTwoPOBCalls() {
		when(trueProcessorMock.shouldProcess(any())).thenReturn(true);
		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var externalCaseId = "External-12345";
		final var noteText = "A minor note";
		final var responsibleGroup = "responsibleGroup";
		final var caseCategory = "caseCategory";
		final var caseStatus = "Delivered";
		final var noteType = NoteType.PROBLEM;
		final var request = UpdateCaseRequest.create()
			.withCaseCategory(caseCategory)
			.withCaseStatus(caseStatus)
			.withExternalCaseId(externalCaseId)
			.withNote(Note.create()
				.withText(noteText)
				.withType(noteType))
			.withResponsibleGroup(responsibleGroup);

		// Call (this will trigger two calls to POB)
		caseService.updateCase(pobKey, caseId, request);

		// Verification
		verify(configurationServiceMock).validateCaseCategory(pobKey, caseCategory);
		verify(pobClientMock, times(2)).updateCase(eq(pobKey), pobPayloadCaptor.capture());
		processorListSpy.forEach(processor -> verify(processor, atLeastOnce()).shouldProcess(request));
		verify(falseProcessorMock, never()).preProcess(any(), any(), any(), any());
		verify(falseProcessorMock, never()).postProcess(any(), any(), any(), any());
		verify(trueProcessorMock, times(2)).preProcess(eq(pobKey), eq(caseId), eq(request), any(PobPayload.class));
		verify(trueProcessorMock, times(2)).postProcess(eq(pobKey), eq(caseId), eq(request), any(PobPayload.class));
		verifyNoMoreInteractions(pobClientMock);

		final var pobPayloadValues = pobPayloadCaptor.getAllValues();
		assertThat(pobPayloadValues).hasSize(2);

		// Assert first call.
		assertThat(pobPayloadValues.get(0).getType()).isEqualTo(DEFAULT_TYPE);
		assertThat(pobPayloadValues.get(0).getData()).isNotNull()
			.containsEntry(KEY_ID, caseId)
			.containsEntry(KEY_CASE_CATEGORY, caseCategory)
			.containsEntry(KEY_CASE_STATUS, CUSTOM_STATUS_MAP.get(caseStatus).get(0).getAttributes().get(KEY_CASE_STATUS))
			.containsEntry(KEY_CLOSURE_CODE, CUSTOM_STATUS_MAP.get(caseStatus).get(0).getAttributes().get(KEY_CLOSURE_CODE)) // First call contain closure code.
			.containsEntry(KEY_EXTERNAL_CASE_ID, externalCaseId)
			.containsEntry(KEY_RESPONSIBLE_GROUP, responsibleGroup)
			.containsEntry(KEY_RESPONSIBLE, null);
		assertThat(pobPayloadValues.get(0).getMemo()).isNotNull();
		assertThat(pobPayloadValues.get(0).getMemo().get(CUSTOM_STATUS_MAP.get(caseStatus).get(0).getStatusNoteType().toValue()).getMemo()).isEqualTo("Beställning levererad");
		assertThat(pobPayloadValues.get(0).getMemo().get(NoteType.PROBLEM.toValue()).getMemo()).isEqualTo(noteText);

		// Assert second call.
		assertThat(pobPayloadValues.get(1).getType()).isEqualTo(DEFAULT_TYPE);
		assertThat(pobPayloadValues.get(1).getData()).isNotNull()
			.containsEntry(KEY_ID, caseId)
			.containsEntry(KEY_CASE_CATEGORY, caseCategory)
			.containsEntry(KEY_CASE_STATUS, CUSTOM_STATUS_MAP.get(caseStatus).get(1).getAttributes().get(KEY_CASE_STATUS))
			.containsEntry(KEY_EXTERNAL_CASE_ID, externalCaseId)
			.containsEntry(KEY_RESPONSIBLE_GROUP, responsibleGroup)
			.containsEntry(KEY_RESPONSIBLE, null)
			.doesNotContainKey(KEY_CLOSURE_CODE); // Second call doesn't contain closure code.
	}

	@Test
	void updateCaseValidationFailsDueToBadCaseCategory() {
		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var externalCaseId = "External-12345";
		final var noteText = "Note";
		final var responsibleGroup = "responsibleGroup";
		final var caseCategory = "caseCategory";
		final var caseStatus = "In Process";
		final var closureCode = "closureCode";
		final var noteType = NoteType.PROBLEM;
		final var request = UpdateCaseRequest.create()
			.withCaseCategory(caseCategory)
			.withCaseStatus(caseStatus)
			.withClosureCode(closureCode)
			.withExternalCaseId(externalCaseId)
			.withNote(Note.create()
				.withText(noteText)
				.withType(noteType))
			.withResponsibleGroup(responsibleGroup);

		// Mock
		doThrow(Problem.valueOf(Status.NOT_FOUND, "Could not find caseCategory")).when(configurationServiceMock).validateCaseCategory(pobKey, caseCategory);

		// Call
		assertThrows(ThrowableProblem.class, () -> caseService.updateCase(pobKey, caseId, request));

		// Verification
		verify(configurationServiceMock).validateCaseCategory(pobKey, caseCategory);
		verify(configurationServiceMock, never()).validateClosureCode(any(), any());
		verifyNoInteractions(pobClientMock, falseProcessorMock,  trueProcessorMock);
	}

	@Test
	void updateCaseValidationFailsDueToBadClosureCode() {

		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var externalCaseId = "External-12345";
		final var noteText = "Note";
		final var responsibleGroup = "responsibleGroup";
		final var caseCategory = "caseCategory";
		final var caseStatus = "In Process";
		final var closureCode = "closureCode";
		final var noteType = NoteType.PROBLEM;
		final var request = UpdateCaseRequest.create()
			.withCaseCategory(caseCategory)
			.withCaseStatus(caseStatus)
			.withClosureCode(closureCode)
			.withExternalCaseId(externalCaseId)
			.withNote(Note.create()
				.withText(noteText)
				.withType(noteType))
			.withResponsibleGroup(responsibleGroup);

		// Mock
		doThrow(Problem.valueOf(Status.NOT_FOUND, "Could not find closureCode")).when(configurationServiceMock).validateClosureCode(pobKey, closureCode);

		// Call
		assertThrows(ThrowableProblem.class, () -> caseService.updateCase(pobKey, caseId, request));

		// Verification
		verify(configurationServiceMock).validateCaseCategory(pobKey, caseCategory);
		verify(configurationServiceMock).validateClosureCode(pobKey, closureCode);
		verifyNoInteractions(pobClientMock, falseProcessorMock,  trueProcessorMock);
	}
	
	/**
	 * Dummy test processors for verifying calls to processor methods 
	 */
	
	private static class FalseProcessor implements ProcessorInterface {
		
		@Override
		public boolean shouldProcess(UpdateCaseRequest updateCaseRequest) {
			return false;
		}
		
		@Override
		public void preProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		}
		
		@Override
		public void postProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		}
	}

	private static class TrueProcessor implements ProcessorInterface {
		
		@Override
		public boolean shouldProcess(UpdateCaseRequest updateCaseRequest) {
			return true;
		}
		
		@Override
		public void preProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		}
		
		@Override
		public void postProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		}
	}
}
