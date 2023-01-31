package se.sundsvall.supportcenter.service.cube;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.ASSIGN_BACK;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.DEFAULT_TYPE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_ACTION_NEEDED;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_ACTION_NEEDED_DESCRIPTION;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_EXTERNAL_CASE_ID;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_ID;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_RESPONSIBLE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_RESPONSIBLE_GROUP;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import generated.client.pob.PobPayload;
import se.sundsvall.supportcenter.api.model.Note;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.api.model.enums.NoteType;
import se.sundsvall.supportcenter.integration.pob.POBClient;
import se.sundsvall.supportcenter.service.CaseService;
import se.sundsvall.supportcenter.service.ConfigurationService;
import se.sundsvall.supportcenter.service.processor.CaseStatusProcessor;
import se.sundsvall.supportcenter.service.processor.ProcessorInterface;

/**
 * Class for testing that cube support flow logic produces valid POB payload when setting status of case to 'AssignBack'
 * 
 * Valid payload example is (memo is optional):
 * 
 *	{
 *		"type": "Case",
 *		"links": [],
 *		"data": {
 *			"Virtual.ExternalCaseId": "External-12345",
 *			"Virtual.ActionNeeded": "true",
 *			"Virtual.ActionNeededDescription": "Åtgärdsbeskrivning i interna anteckningar",
 *			"Id": "12345",
 *			"Responsible": null,
 *			"ResponsibleGroup": "Second Line IT"
 *		},
 *		"memo": {
 *			"CaseInternalNotesCustom": {
 *				"extension": ".html",
 *				"isValidForWeb": false,
 *				"style": 2,
 *				"memo": "Åter för ytterligare granskning",
 *				"handleSeparators": true
 *			}
 *		}
 *	}
 */
@ExtendWith(MockitoExtension.class)
class UpdateCaseAssignBackTest {

	@Mock
	private POBClient pobClientMock;

	@Mock
	private ConfigurationService configurationServiceMock;

	@Spy
	private ProcessorInterface processorSpy = new CaseStatusProcessor();
	
	@Spy
	private List<ProcessorInterface> processorListSpy = new ArrayList<>();
	
	@InjectMocks
	private CaseService caseService;

	@Captor
	private ArgumentCaptor<PobPayload> pobPayloadCaptor;

	@BeforeEach
	void setup() {
		processorListSpy.clear();
		processorListSpy.add(processorSpy);
	}
	
	@Test
	void updateCaseForKnownCubeStatuses() {
		// Parameter values
		final var pobKey = "pobKey";
		final var caseId = "12345";
		final var externalCaseId = "External-12345";
		final var noteText = "A minor note";
		final var request = UpdateCaseRequest.create()
			.withExternalCaseId(externalCaseId)
			.withCaseStatus(ASSIGN_BACK.getValue())
			.withNote(Note.create()
				.withText(noteText)
				.withType(NoteType.WORKNOTE));

		// Call
		caseService.updateCase(pobKey, caseId, request);

		// Verifications and assertions
		verify(processorSpy, times(2)).shouldProcess(request);
		verify(processorSpy).preProcess(eq(pobKey), eq(caseId), eq(request), any(PobPayload.class));
		verify(processorSpy).postProcess(eq(pobKey), eq(caseId), eq(request), any(PobPayload.class));
		verify(pobClientMock).updateCase(eq(pobKey), pobPayloadCaptor.capture());
		verifyNoMoreInteractions(pobClientMock, processorSpy);
		verifyNoInteractions(configurationServiceMock);
		
		final var pobPayloadValue = pobPayloadCaptor.getValue();
		assertThat(pobPayloadValue).isNotNull();
		assertThat(pobPayloadValue.getType()).isEqualTo(DEFAULT_TYPE);
		assertThat(pobPayloadValue.getData()).isNotNull().hasSize(6)
			.containsEntry(KEY_ID, caseId)
			.containsEntry(KEY_EXTERNAL_CASE_ID, externalCaseId)
			.containsEntry(KEY_RESPONSIBLE_GROUP, "Second Line IT")
			.containsEntry(KEY_RESPONSIBLE, null)
			.containsEntry(KEY_ACTION_NEEDED, true)
			.containsEntry(KEY_ACTION_NEEDED_DESCRIPTION, "Åtgärdsbeskrivning i interna anteckningar");
		assertThat(pobPayloadValue.getMemo()).isNotNull();
		assertThat(pobPayloadValue.getMemo().get(NoteType.WORKNOTE.toValue()).getMemo()).isEqualTo(noteText);
	}
}
