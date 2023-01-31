package se.sundsvall.supportcenter.api.model;

import java.util.Objects;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import se.sundsvall.supportcenter.api.model.enums.NoteType;

@Schema(description = "Note model")
public class Note {

	@Schema(required = true)
	@NotNull(message = "must be provided")
	private NoteType type;

	@Schema(description = "Note text", required = true, example = "This is a note")
	@NotBlank(message = "must be provided")
	private String text;

	public static Note create() {
		return new Note();
	}

	public NoteType getType() {
		return type;
	}

	public void setType(NoteType type) {
		this.type = type;
	}

	public Note withType(NoteType type) {
		this.type = type;
		return this;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Note withText(String text) {
		this.text = text;
		return this;
	}

	@Override
	public int hashCode() { return Objects.hash(text, type); }

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Note other = (Note) obj;
		return Objects.equals(text, other.text) && type == other.type;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Note [type=").append(type).append(", text=").append(text).append("]");
		return builder.toString();
	}
}
