package se.sundsvall.supportcenter.service.mapper;

import generated.client.pob.PobMemo;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportcenter.api.model.Note;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.api.model.enums.NoteType;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.CUSTOM_STATUS_MAP;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.DEFAULT_TYPE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CASE_CATEGORY;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CASE_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CLOSURE_CODE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_EXTERNAL_CASE_ID;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_ID;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_RESPONSIBLE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_RESPONSIBLE_GROUP;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_SHOP_CI_NAME;

class UpdateCaseMapperTest {

	@Test
	void toPobPayload() {

		// Parameter values.
		final var caseId = "caseId";
		final var caseCategory = "caseCategory";
		final var caseStatus = "Opened";
		final var closureCode = "closureCode";
		final var externalCaseId = "externalCaseId";
		final var hardwareName = "hardwareName";
		final var noteText = "noteText";
		final var noteType = NoteType.PROBLEM;
		final var responsibleGroup = "responsibleGroup";

		final var updateCaseRequest = UpdateCaseRequest.create()
			.withCaseCategory(caseCategory)
			.withCaseStatus(caseStatus)
			.withClosureCode(closureCode)
			.withExternalCaseId(externalCaseId)
			.withHardwareName(hardwareName)
			.withNote(Note.create()
				.withText(noteText)
				.withType(noteType))
			.withResponsibleGroup(responsibleGroup);

		// Call
		final var resultList = UpdateCaseMapper.toPobPayloads(caseId, updateCaseRequest);

		// Verification
		assertThat(resultList).hasSize(1);
		final var result = resultList.stream().findFirst().get();

		assertThat(result).isNotNull();
		assertThat(result.getLinks()).isNotNull().isEmpty();
		assertThat(result.getType()).isEqualTo(DEFAULT_TYPE);
		assertThat(result.getData()).isNotNull()
			.containsEntry(KEY_ID, caseId)
			.containsEntry(KEY_CASE_CATEGORY, caseCategory)
			.containsEntry(KEY_CASE_STATUS, caseStatus)
			.containsEntry(KEY_CLOSURE_CODE, closureCode)
			.containsEntry(KEY_EXTERNAL_CASE_ID, externalCaseId)
			.containsEntry(KEY_SHOP_CI_NAME, hardwareName)
			.containsEntry(KEY_RESPONSIBLE_GROUP, responsibleGroup)
			.containsEntry(KEY_RESPONSIBLE, null);

		// Two different notes.
		assertThat(result.getMemo()).isNotNull().hasSize(2);
		assertThat(result.getMemo().get(NoteType.PROBLEM.toValue())).isNotNull();
		assertThat(result.getMemo().get(NoteType.PROBLEM.toValue()).getMemo()).isEqualTo(noteText);
		assertThat(result.getMemo().get(NoteType.PROBLEM.toValue()).getExtension()).isEqualTo(".html");
		assertThat(result.getMemo().get(NoteType.PROBLEM.toValue()).getHandleSeparators()).isTrue();
		assertThat(result.getMemo().get(NoteType.PROBLEM.toValue()).getIsValidForWeb()).isFalse();
		assertThat(result.getMemo().get(NoteType.PROBLEM.toValue()).getStyle()).isEqualTo(PobMemo.StyleEnum.NUMBER_2);
		assertThat(result.getMemo().get(NoteType.WORKNOTE.toValue())).isNotNull();
		assertThat(result.getMemo().get(NoteType.WORKNOTE.toValue()).getMemo()).isEqualTo("Status: 'Opened'");
		assertThat(result.getMemo().get(NoteType.WORKNOTE.toValue()).getExtension()).isEqualTo(".html");
		assertThat(result.getMemo().get(NoteType.WORKNOTE.toValue()).getHandleSeparators()).isTrue();
		assertThat(result.getMemo().get(NoteType.WORKNOTE.toValue()).getIsValidForWeb()).isFalse();
		assertThat(result.getMemo().get(NoteType.WORKNOTE.toValue()).getStyle()).isEqualTo(PobMemo.StyleEnum.NUMBER_2);
	}

	@Test
	void toPobPayloadsWhenStatusIsDelivered() {

		// Parameter values.
		final var caseId = "caseId";
		final var caseCategory = "caseCategory";
		final var caseStatus = "Delivered";
		final var closureCode = "closureCode";
		final var externalCaseId = "externalCaseId";
		final var hardwareName = "hardwareName";
		final var noteText = "noteText";
		final var noteType = NoteType.PROBLEM;
		final var responsibleGroup = "responsibleGroup";

		final var updateCaseRequest = UpdateCaseRequest.create()
			.withCaseCategory(caseCategory)
			.withCaseStatus(caseStatus)
			.withClosureCode(closureCode)
			.withExternalCaseId(externalCaseId)
			.withHardwareName(hardwareName)
			.withNote(Note.create()
				.withText(noteText)
				.withType(noteType))
			.withResponsibleGroup(responsibleGroup);

		// Call
		final var resultList = UpdateCaseMapper.toPobPayloads(caseId, updateCaseRequest);

		// Verification
		assertThat(resultList).hasSize(2);
		final var firstResult = resultList.getFirst();
		final var secondResult = resultList.getLast();

		// Assert first call.
		assertThat(firstResult.getType()).isEqualTo(DEFAULT_TYPE);
		assertThat(firstResult.getData()).isNotNull()
			.containsEntry(KEY_ID, caseId)
			.containsEntry(KEY_CASE_CATEGORY, caseCategory)
			.containsEntry(KEY_CASE_STATUS, CUSTOM_STATUS_MAP.get(caseStatus).getFirst().getAttributes().get(KEY_CASE_STATUS))
			.containsEntry(KEY_CLOSURE_CODE, CUSTOM_STATUS_MAP.get(caseStatus).getFirst().getAttributes().get(KEY_CLOSURE_CODE)) // First call contain closure code.
			.containsEntry(KEY_EXTERNAL_CASE_ID, externalCaseId)
			.containsEntry(KEY_RESPONSIBLE_GROUP, responsibleGroup)
			.containsEntry(KEY_RESPONSIBLE, null);

		// Two different notes.
		assertThat(firstResult.getMemo()).isNotNull().hasSize(2);
		assertThat(firstResult.getMemo()).isNotNull();
		assertThat(firstResult.getMemo().get(NoteType.PROBLEM.toValue())).isNotNull();
		assertThat(firstResult.getMemo().get(NoteType.PROBLEM.toValue()).getMemo()).isEqualTo(noteText);
		assertThat(firstResult.getMemo().get(NoteType.PROBLEM.toValue()).getExtension()).isEqualTo(".html");
		assertThat(firstResult.getMemo().get(NoteType.PROBLEM.toValue()).getHandleSeparators()).isTrue();
		assertThat(firstResult.getMemo().get(NoteType.PROBLEM.toValue()).getIsValidForWeb()).isFalse();
		assertThat(firstResult.getMemo().get(NoteType.PROBLEM.toValue()).getStyle()).isEqualTo(PobMemo.StyleEnum.NUMBER_2);
		assertThat(firstResult.getMemo().get(CUSTOM_STATUS_MAP.get(caseStatus).getFirst().getStatusNoteType().toValue()).getMemo()).isEqualTo("Beställning levererad");
		assertThat(firstResult.getMemo().get(CUSTOM_STATUS_MAP.get(caseStatus).getFirst().getStatusNoteType().toValue()).getExtension()).isEqualTo(".html");
		assertThat(firstResult.getMemo().get(CUSTOM_STATUS_MAP.get(caseStatus).getFirst().getStatusNoteType().toValue()).getHandleSeparators()).isTrue();
		assertThat(firstResult.getMemo().get(CUSTOM_STATUS_MAP.get(caseStatus).getFirst().getStatusNoteType().toValue()).getIsValidForWeb()).isFalse();
		assertThat(firstResult.getMemo().get(CUSTOM_STATUS_MAP.get(caseStatus).getFirst().getStatusNoteType().toValue()).getStyle()).isEqualTo(PobMemo.StyleEnum.NUMBER_2);

		// Assert second call.
		assertThat(secondResult.getType()).isEqualTo(DEFAULT_TYPE);
		assertThat(secondResult.getData()).isNotNull()
			.containsEntry(KEY_ID, caseId)
			.containsEntry(KEY_CASE_CATEGORY, caseCategory)
			.containsEntry(KEY_CASE_STATUS, CUSTOM_STATUS_MAP.get(caseStatus).get(1).getAttributes().get(KEY_CASE_STATUS))
			.containsEntry(KEY_CLOSURE_CODE, closureCode) // Contains the original closure code (not overwritten)
			.containsEntry(KEY_EXTERNAL_CASE_ID, externalCaseId)
			.containsEntry(KEY_RESPONSIBLE_GROUP, responsibleGroup)
			.containsEntry(KEY_RESPONSIBLE, null);
	}

	@Test
	void toPobPayloadWhenNoteIsNull() {

		// Parameter values.
		final var caseId = "caseId";
		final var caseCategory = "caseCategory";
		final var closureCode = "closureCode";
		final var externalCaseId = "externalCaseId";
		final var hardwareName = "hardwareName";
		final var responsibleGroup = "responsibleGroup";

		final var updateCaseRequest = UpdateCaseRequest.create()
			.withCaseCategory(caseCategory)
			.withClosureCode(closureCode)
			.withExternalCaseId(externalCaseId)
			.withHardwareName(hardwareName)
			.withResponsibleGroup(responsibleGroup);

		// Call
		final var resultList = UpdateCaseMapper.toPobPayloads(caseId, updateCaseRequest);

		// Verification
		assertThat(resultList).hasSize(1);
		final var result = resultList.stream().findFirst().get();

		assertThat(result).isNotNull();
		assertThat(result.getLinks()).isNotNull().isEmpty();
		assertThat(result.getType()).isEqualTo(DEFAULT_TYPE);
		assertThat(result.getData()).isNotNull()
			.containsEntry(KEY_ID, caseId)
			.containsEntry(KEY_CASE_CATEGORY, caseCategory)
			.containsEntry(KEY_CLOSURE_CODE, closureCode)
			.containsEntry(KEY_EXTERNAL_CASE_ID, externalCaseId)
			.containsEntry(KEY_SHOP_CI_NAME, hardwareName)
			.containsEntry(KEY_RESPONSIBLE_GROUP, responsibleGroup)
			.containsEntry(KEY_RESPONSIBLE, null);
		assertThat(result.getMemo()).isNull();
	}

	@Test
	void toPobPayloadWhenNoteIsNullAndStatusProvided() {

		// Parameter values.
		final var caseId = "caseId";
		final var caseCategory = "caseCategory";
		final var caseStatus = "Opened";
		final var closureCode = "closureCode";
		final var externalCaseId = "externalCaseId";
		final var hardwareName = "hardwareName";
		final var responsibleGroup = "responsibleGroup";

		final var updateCaseRequest = UpdateCaseRequest.create()
			.withCaseCategory(caseCategory)
			.withCaseStatus(caseStatus)
			.withClosureCode(closureCode)
			.withExternalCaseId(externalCaseId)
			.withHardwareName(hardwareName)
			.withResponsibleGroup(responsibleGroup);

		// Call
		final var resultList = UpdateCaseMapper.toPobPayloads(caseId, updateCaseRequest);

		// Verification
		assertThat(resultList).hasSize(1);
		final var result = resultList.stream().findFirst().get();

		assertThat(result).isNotNull();
		assertThat(result.getLinks()).isNotNull().isEmpty();
		assertThat(result.getType()).isEqualTo(DEFAULT_TYPE);
		assertThat(result.getData()).isNotNull()
			.containsEntry(KEY_ID, caseId)
			.containsEntry(KEY_CASE_CATEGORY, caseCategory)
			.containsEntry(KEY_CASE_STATUS, caseStatus)
			.containsEntry(KEY_CLOSURE_CODE, closureCode)
			.containsEntry(KEY_EXTERNAL_CASE_ID, externalCaseId)
			.containsEntry(KEY_SHOP_CI_NAME, hardwareName)
			.containsEntry(KEY_RESPONSIBLE_GROUP, responsibleGroup)
			.containsEntry(KEY_RESPONSIBLE, null);
		assertThat(result.getMemo()).isNotNull();
		assertThat(result.getMemo().get(NoteType.WORKNOTE.toValue())).isNotNull();
		assertThat(result.getMemo().get(NoteType.WORKNOTE.toValue()).getMemo()).isEqualTo("Status: 'Opened'");
		assertThat(result.getMemo().get(NoteType.WORKNOTE.toValue()).getExtension()).isEqualTo(".html");
		assertThat(result.getMemo().get(NoteType.WORKNOTE.toValue()).getHandleSeparators()).isTrue();
		assertThat(result.getMemo().get(NoteType.WORKNOTE.toValue()).getIsValidForWeb()).isFalse();
		assertThat(result.getMemo().get(NoteType.WORKNOTE.toValue()).getStyle()).isEqualTo(PobMemo.StyleEnum.NUMBER_2);
	}

	@Test
	void toPobPayloadWhenNoteIsProvidedWithSameTypeAsTheStatusNote() {

		// Parameter values.
		final var caseId = "caseId";
		final var caseStatus = "Opened";
		final var noteText = "noteText";
		final var noteType = NoteType.WORKNOTE;

		final var updateCaseRequest = UpdateCaseRequest.create()
			.withCaseStatus(caseStatus)
			.withNote(Note.create()
				.withText(noteText)
				.withType(noteType));

		// Call
		final var resultList = UpdateCaseMapper.toPobPayloads(caseId, updateCaseRequest);

		// Verification
		assertThat(resultList).hasSize(1);
		final var result = resultList.stream().findFirst().get();

		assertThat(result).isNotNull();
		assertThat(result.getLinks()).isNotNull().isEmpty();
		assertThat(result.getType()).isEqualTo(DEFAULT_TYPE);
		assertThat(result.getData()).isNotNull()
			.containsEntry(KEY_ID, caseId)
			.containsEntry(KEY_CASE_STATUS, caseStatus);
		assertThat(result.getMemo()).isNotNull().hasSize(1);
		assertThat(result.getMemo().get(NoteType.WORKNOTE.toValue())).isNotNull();
		assertThat(result.getMemo().get(NoteType.WORKNOTE.toValue()).getMemo()).isEqualTo(noteText + " | Status: 'Opened'"); // Status note is appended to existing note.
		assertThat(result.getMemo().get(NoteType.WORKNOTE.toValue()).getExtension()).isEqualTo(".html");
		assertThat(result.getMemo().get(NoteType.WORKNOTE.toValue()).getHandleSeparators()).isTrue();
		assertThat(result.getMemo().get(NoteType.WORKNOTE.toValue()).getIsValidForWeb()).isFalse();
		assertThat(result.getMemo().get(NoteType.WORKNOTE.toValue()).getStyle()).isEqualTo(PobMemo.StyleEnum.NUMBER_2);
	}

	@Test
	void toPobPayloadResponsibleIsNotSetWhenNotNeeded() {

		// Parameter values.
		final var caseId = "caseId";

		final var updateCaseRequest = UpdateCaseRequest.create();

		// Call
		final var resultList = UpdateCaseMapper.toPobPayloads(caseId, updateCaseRequest);

		// Verification
		assertThat(resultList).hasSize(1);
		final var result = resultList.stream().findFirst().get();

		assertThat(result).isNotNull();
		assertThat(result.getLinks()).isNotNull().isEmpty();
		assertThat(result.getType()).isEqualTo(DEFAULT_TYPE);
		assertThat(result.getData()).isNotNull()
			.doesNotContainKey(KEY_RESPONSIBLE_GROUP)
			.doesNotContainKey(KEY_RESPONSIBLE);
		assertThat(result.getMemo()).isNull();
	}
}
