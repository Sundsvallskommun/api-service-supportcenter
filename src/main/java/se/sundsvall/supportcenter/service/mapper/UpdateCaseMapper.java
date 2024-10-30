package se.sundsvall.supportcenter.service.mapper;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static se.sundsvall.supportcenter.service.mapper.CommonMapper.toMemo;
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
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.NOTE_DELIVERED;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.NOTE_STATUS_PART;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.STATUS_DELIVERED;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import generated.client.pob.PobMemo;
import generated.client.pob.PobPayload;
import se.sundsvall.supportcenter.api.model.Note;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.api.model.enums.NoteType;

public class UpdateCaseMapper {

	private UpdateCaseMapper() {}

	public static List<PobPayload> toPobPayloads(String caseId, UpdateCaseRequest updateCaseRequest) {

		if (isNotBlank(updateCaseRequest.getCaseStatus())) {
			return toPobPayloadsWhenStatusIsPresent(caseId, updateCaseRequest);
		}

		return List.of(new PobPayload()
			.links(emptyList())
			.type(DEFAULT_TYPE)
			.data(toData(caseId, updateCaseRequest))
			.memo(toMemo(updateCaseRequest.getNote())));
	}

	private static List<PobPayload> toPobPayloadsWhenStatusIsPresent(String caseId, UpdateCaseRequest updateCaseRequest) {

		// Fetch custom mapping for this status.
		final var customStatusMappings = CUSTOM_STATUS_MAP.get(updateCaseRequest.getCaseStatus());
		if (nonNull(customStatusMappings)) {

			// There is a custom mapping for this status.
			return customStatusMappings.stream()
				.map(customStatusMapping -> {

					// Create data-map and put all extra attributes in this map (overwrite if the keys already exists).
					final var dataMap = toData(caseId, updateCaseRequest);
					dataMap.putAll(customStatusMapping.getAttributes());

					// Create note if status note type has been set in the custom mapping in CaseMapperConstants class
					final var statusNote = createStatusNote(customStatusMapping.getStatusNoteType(), updateCaseRequest.getCaseStatus());

					return new PobPayload()
						.links(emptyList())
						.type(DEFAULT_TYPE)
						.data(dataMap)
						.memo(toMemos(statusNote, toMemo(updateCaseRequest.getNote())));
				})
				.toList();
		}
		// There was no custom mapping for this status. Create a PobPayload with a worknote containing the status.
		return List.of(new PobPayload()
			.links(emptyList())
			.type(DEFAULT_TYPE)
			.data(toData(caseId, updateCaseRequest))
			.memo(toMemos(createStatusNote(NoteType.WORKNOTE, updateCaseRequest.getCaseStatus()), toMemo(updateCaseRequest.getNote()))));
	}

	private static Map<String, Object> toData(String caseId, UpdateCaseRequest updateCaseRequest) {
		final var dataMap = new HashMap<String, Object>();
		ofNullable(caseId).ifPresent(value -> dataMap.put(KEY_ID, value));
		ofNullable(updateCaseRequest.getCaseCategory()).ifPresent(value -> dataMap.put(KEY_CASE_CATEGORY, value));
		ofNullable(updateCaseRequest.getCaseStatus()).ifPresent(value -> dataMap.put(KEY_CASE_STATUS, value));
		ofNullable(updateCaseRequest.getClosureCode()).ifPresent(value -> dataMap.put(KEY_CLOSURE_CODE, value));
		ofNullable(updateCaseRequest.getHardwareName()).ifPresent(value -> dataMap.put(KEY_SHOP_CI_NAME, value));
		ofNullable(updateCaseRequest.getExternalCaseId()).ifPresent(value -> dataMap.put(KEY_EXTERNAL_CASE_ID, value));
		ofNullable(updateCaseRequest.getResponsibleGroup()).ifPresent(value -> {
			dataMap.put(KEY_RESPONSIBLE, null); // Responsible must be cleared if ResponsibleGroup is set.
			dataMap.put(KEY_RESPONSIBLE_GROUP, value);
		});
		return dataMap;
	}

	/**
	 * Creates a Map of PobMemo:s from a Note.
	 * 
	 * If an existing map is provided, the new note will be merged into according to this algorithm:
	 * 
	 * <pre>
	 * - If a PobMemo exists with the same type as the provided Note: append note to existing text.
	 * - If no PobMemo exists with the same type as the provided Note: Create new PobMemo and add to the returned map.
	 * </pre>
	 * 
	 * @param  note  the note to convert into a PobMemo.
	 * @param  memos a Map with any existing (newly created) PobMemo:s.
	 * @return       a new Map with the created PobMemo and all existing ones (i.e. already added in the provided map).
	 */
	private static Map<String, PobMemo> toMemos(Note note, Map<String, PobMemo> memos) {
		if (isNull(note))
			return memos; // If note is null, just return incoming memo map

		final var memoMap = new HashMap<>(ofNullable(memos).orElse(emptyMap()));
		final var memoType = note.getType().toValue();

		// Fetch existing memo by note-type (if any)
		ofNullable(memoMap.get(memoType)).ifPresentOrElse(

			// If a note with this memo-key is present: append the new note to the existing text.
			existingMemo -> existingMemo.memo(existingMemo.getMemo() + " | " + note.getText()),

			// Else: create a new PobMemo and add it to existing memo-map.
			() -> memoMap.putAll(toMemo(note)));

		return memoMap;
	}

	/**
	 * Creates a note of note type
	 * 
	 * @param  noteType   the notetype to use
	 * @param  caseStatus the status to be used
	 * @return            a note instance representing sent in parameters
	 */
	private static Note createStatusNote(NoteType noteType, String caseStatus) {
		if (isNull(noteType)) {
			return null;
		} else if (STATUS_DELIVERED.equals(caseStatus)) {
			return Note.create().withType(noteType).withText(NOTE_DELIVERED);
		}

		return Note.create().withType(noteType).withText(format(NOTE_STATUS_PART, caseStatus));
	}
}
