package se.sundsvall.supportcenter.service.mapper;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportcenter.service.mapper.CommonMapper.toMemo;
import static se.sundsvall.supportcenter.service.mapper.CommonMapper.toNote;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import generated.client.pob.PobMemo;
import generated.client.pob.PobMemo.StyleEnum;
import generated.client.pob.PobPayload;
import se.sundsvall.supportcenter.api.model.Note;
import se.sundsvall.supportcenter.api.model.enums.NoteType;

class CommonMapperTest {

	@Test
	void testToMemoWhenNoteNull() {
		assertThat(toMemo(null)).isNull();
	}
	
	@ParameterizedTest
	@EnumSource(value = NoteType.class)
	void testToMemoForEachNoteType(NoteType type) {
		final var noteText = format("noteText for type %s", type.toValue());
		final var note = Note.create().withText(noteText).withType(type);
		
		assertThat(toMemo(note)).containsKey(type.toValue())
		.extractingByKey(type.toValue()).extracting(
			PobMemo::getExtension, 
			PobMemo::getHandleSeparators,
			PobMemo::getIsValidForWeb,
			PobMemo::getMemo,
			PobMemo::getStyle)
		.containsExactly(
			".html", 
			true,
			false,
			noteText,
			type == NoteType.SUPPLIERNOTE ? StyleEnum.NUMBER_0 : StyleEnum.NUMBER_2);
	}
	
	@Test
	void testToNoteWhenEmptyMemoMap() {
		assertThat(toNote(new PobPayload())).isNull();
	}

	@ParameterizedTest
	@EnumSource(value = NoteType.class)
	void testToNoteWhenEmptyText(NoteType type) {
		assertThat(toNote(new PobPayload().memo(Map.of(type.toValue(), new PobMemo()))).getText()).isNull();
	}
	
	@ParameterizedTest
	@EnumSource(value = NoteType.class)
	void testToNoteForEachNoteType(NoteType type) {
		final var noteText = format("noteText for type %s", type.toValue());
		assertThat(toNote(new PobPayload().memo(Map.of(type.toValue(), new PobMemo().memo(noteText)))).getText()).isEqualTo(noteText);
		
	}
}
