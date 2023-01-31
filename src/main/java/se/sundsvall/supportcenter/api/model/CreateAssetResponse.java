package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(description = "CreateAssetResponse model")
public class CreateAssetResponse {

	@Schema(example = "111222", description = "Id of asset in Pob")
	private String id;

	public static CreateAssetResponse create() {
		return new CreateAssetResponse();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public CreateAssetResponse withId(String id) {
		this.id = id;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CreateAssetResponse that = (CreateAssetResponse) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CreateAssetResponse [id=").append(id).append("]");
		return builder.toString();
	}
}
