package se.sundsvall.supportcenter.service.mapper;

import generated.client.pob.PobMemo;
import generated.client.pob.PobMemo.StyleEnum;
import generated.client.pob.PobPayload;
import se.sundsvall.supportcenter.api.model.Note;
import se.sundsvall.supportcenter.api.model.enums.NoteType;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportcenter.api.model.enums.NoteType.SUPPLIERNOTE;
import static se.sundsvall.supportcenter.service.util.CaseUtil.extractValueFromJsonPath;
import static se.sundsvall.supportcenter.service.util.CaseUtil.jsonPathExists;

public class CommonMapper {
	
	private CommonMapper() {}
	
	private static final String DEFAULT_NOTE_EXTENSION = ".html";
	private static final boolean DEFAULT_NOTE_HANDLE_SEPARATORS = true;
	private static final boolean DEFAULT_NOTE_IS_VALID_FOR_WEB = false;
	private static final StyleEnum DEFAULT_NOTE_STYLE = StyleEnum.NUMBER_2;
	
	private static final String NOTE_TEXT_JSON_PATH = "$['Memo']['%s']['Memo']";
	/**
	 * Method for creating a map of PobMemo:s containing sent in note.
	 * 
	 * @param note the note to convert into a PobMemo.
	 * @return a Map with the created PobMemo.
	 */
	public static Map<String, PobMemo> toMemo(Note note) {
		if (isNull(note)) {
			return null;
		}

		final var memoType = note.getType().toValue();

		return Map.of(memoType, new PobMemo()
			.extension(DEFAULT_NOTE_EXTENSION)
			.handleSeparators(DEFAULT_NOTE_HANDLE_SEPARATORS)
			.isValidForWeb(DEFAULT_NOTE_IS_VALID_FOR_WEB)
			.style(toNoteStyle(note.getType()))
			.memo(note.getText()));
	}

	/**
	 * Method for creating a map of PobMemo:s containing sent in note.
	 *
	 * @param note the note to convert into a PobMemo.
	 * @param isValidForWeb if the note is valid for web
	 * @return a Map with the created PobMemo.
	 */
	public static Map<String, PobMemo> toMemo(Note note, boolean isValidForWeb) {
		if (isNull(note)) {
			return null;
		}

		final var memoType = note.getType().toValue();

		return Map.of(memoType, new PobMemo()
			.extension(DEFAULT_NOTE_EXTENSION)
			.handleSeparators(DEFAULT_NOTE_HANDLE_SEPARATORS)
			.isValidForWeb(isValidForWeb)
			.style(toNoteStyle(note.getType()))
			.memo(note.getText()));
	}

	private static StyleEnum toNoteStyle(NoteType noteType) {
		if (noteType == SUPPLIERNOTE) {
			return StyleEnum.NUMBER_0;
		}
		return DEFAULT_NOTE_STYLE;
	}
	
	/**
	 * Method for extracting a note from pob payload if it exists
	 * @param pobPayload the pob payload to extract note from
	 * @return note in payload or null
	 */
	public static Note toNote(PobPayload pobPayload) {
		final var memo = pobPayload.getMemo();
		final var noteType = ofNullable(memo)
			.map(Map::keySet)
			.map(Set::stream)
			.map(Stream::findFirst)
			.flatMap(optional -> optional)
			.map(NoteType::forValue)
			.orElse(null);

		if (nonNull(noteType)) {
			return new Note().withType(noteType).withText(getValue(pobPayload, format(NOTE_TEXT_JSON_PATH, noteType.toValue())));
		}
		return null;
	}
	
	private static String getValue(PobPayload pobPayload, String jsonPath) {
		if (jsonPathExists(pobPayload, jsonPath)) {
			return extractValueFromJsonPath(pobPayload, jsonPath, true);
		}
		return null;
	}
}
