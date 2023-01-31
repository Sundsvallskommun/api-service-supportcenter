package se.sundsvall.supportcenter.api.model.enums;

import static java.lang.String.format;
import static java.util.Arrays.stream;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "NoteType model", enumAsRef = true)
public enum NoteType {

	PROBLEM("Problem"),
	SOLUTION("Solution"),
	WORKNOTE("CaseInternalNotesCustom"),
	SUPPLIERNOTE("CILeverantorensAnteckningar");

	private final String value;

	NoteType(String value) {
		this.value = value;
	}

	public String toValue() {
		return this.value;
	}

	public static NoteType forValue(String value) {
		return stream(values())
			.filter(enumObj -> enumObj.value.equalsIgnoreCase(value))
			.findFirst().orElseThrow(() -> new IllegalArgumentException(format("Illegal enum value: '%s'", value)));
	}
}
