package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "EndOfLeaseBatchResponse model")
public class EndOfLeaseBatchResponse {

	@Schema(examples = "8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c", description = "Id of the registered batch")
	private String id;

	public static EndOfLeaseBatchResponse create() {
		return new EndOfLeaseBatchResponse();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public EndOfLeaseBatchResponse withId(String id) {
		this.id = id;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof final EndOfLeaseBatchResponse that))
			return false;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	@Override
	public String toString() {
		return "EndOfLeaseBatchResponse{" +
			"id='" + id + '\'' +
			'}';
	}
}
