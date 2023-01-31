package se.sundsvall.supportcenter.api.model.enums;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static se.sundsvall.supportcenter.api.model.enums.NoteType.PROBLEM;
import static se.sundsvall.supportcenter.api.model.enums.NoteType.SOLUTION;
import static se.sundsvall.supportcenter.api.model.enums.NoteType.WORKNOTE;
import static se.sundsvall.supportcenter.api.model.enums.NoteType.SUPPLIERNOTE;

import org.junit.jupiter.api.Test;

class NoteTypeTest {

	@Test
	void enums() {
		assertThat(NoteType.values())
			.containsExactly(PROBLEM, SOLUTION, WORKNOTE, SUPPLIERNOTE);
	}

	@Test
	void enumValues() {
		assertThat(PROBLEM).hasToString("PROBLEM");
		assertThat(SOLUTION).hasToString("SOLUTION");
		assertThat(WORKNOTE).hasToString("WORKNOTE");
		assertThat(SUPPLIERNOTE).hasToString("SUPPLIERNOTE");

		assertThat(PROBLEM).extracting(NoteType::toValue).isEqualTo("Problem");
		assertThat(SOLUTION).extracting(NoteType::toValue).isEqualTo("Solution");
		assertThat(WORKNOTE).extracting(NoteType::toValue).isEqualTo("CaseInternalNotesCustom");
		assertThat(SUPPLIERNOTE).extracting(NoteType::toValue).isEqualTo("CILeverantorensAnteckningar");
	}

	@Test
	void forValue() {
		stream(NoteType.values()).forEach(noteType -> assertThat(NoteType.forValue(noteType.toValue())).isEqualTo(noteType));
	}

	@Test
	void forValueThrowsException() {
		final var exception = assertThrows(IllegalArgumentException.class, () -> NoteType.forValue("Non-valid"));
		assertThat(exception).hasMessage("Illegal enum value: 'Non-valid'");
	}
}
