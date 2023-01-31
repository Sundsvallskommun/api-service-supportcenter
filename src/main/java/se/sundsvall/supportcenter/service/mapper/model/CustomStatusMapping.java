package se.sundsvall.supportcenter.service.mapper.model;

import java.util.Map;
import java.util.Objects;

import se.sundsvall.supportcenter.api.model.enums.NoteType;

public class CustomStatusMapping {

	/**
	 * Extra attributes that should be added to the PobPayload.
	 */
	private Map<String, Object> attributes;

	/**
	 * The NoteType that should be used for the status note.
	 */
	private NoteType statusNoteType;

	public static CustomStatusMapping create() {
		return new CustomStatusMapping();
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public CustomStatusMapping withAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
		return this;
	}

	public NoteType getStatusNoteType() {
		return statusNoteType;
	}

	public void setStatusNoteType(NoteType statusNoteType) {
		this.statusNoteType = statusNoteType;
	}

	public CustomStatusMapping withStatusNoteType(NoteType statusNoteType) {
		this.statusNoteType = statusNoteType;
		return this;
	}

	@Override
	public int hashCode() { return Objects.hash(statusNoteType, attributes); }

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CustomStatusMapping other = (CustomStatusMapping) obj;
		return statusNoteType == other.statusNoteType && Objects.equals(attributes, other.attributes);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CustomStatusMapping [attributes=").append(attributes).append(", statusNoteType=").append(statusNoteType).append("]");
		return builder.toString();
	}
}
