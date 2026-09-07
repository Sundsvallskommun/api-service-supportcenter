package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "CreateEndOfLeaseBatchRequest model")
public class CreateEndOfLeaseBatchRequest {

	@Schema(examples = "d1f3a8c2-9b7e-4a5f-8c3d-2e6b1a4f7c90", description = "Id of the batch, set by the sender. A batch sent again under an id that is already registered is accepted without being queued a second time", requiredMode = REQUIRED)
	@ValidUuid(nullable = false)
	private String externalBatchId;

	@ArraySchema(schema = @Schema(description = "Computers that have reached end of lease", requiredMode = REQUIRED, implementation = EndOfLeaseComputer.class))
	@NotEmpty(message = "must contain at least one computer")
	private List<@Valid EndOfLeaseComputer> computers;

	public static CreateEndOfLeaseBatchRequest create() {
		return new CreateEndOfLeaseBatchRequest();
	}

	public String getExternalBatchId() {
		return externalBatchId;
	}

	public void setExternalBatchId(String externalBatchId) {
		this.externalBatchId = externalBatchId;
	}

	public CreateEndOfLeaseBatchRequest withExternalBatchId(String externalBatchId) {
		this.externalBatchId = externalBatchId;
		return this;
	}

	public List<EndOfLeaseComputer> getComputers() {
		return computers;
	}

	public void setComputers(List<EndOfLeaseComputer> computers) {
		this.computers = computers;
	}

	public CreateEndOfLeaseBatchRequest withComputers(List<EndOfLeaseComputer> computers) {
		this.computers = computers;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof final CreateEndOfLeaseBatchRequest that))
			return false;
		return Objects.equals(externalBatchId, that.externalBatchId) && Objects.equals(computers, that.computers);
	}

	@Override
	public int hashCode() {
		return Objects.hash(externalBatchId, computers);
	}

	@Override
	public String toString() {
		return "CreateEndOfLeaseBatchRequest{" +
			"externalBatchId='" + externalBatchId + '\'' +
			", computers=" + computers +
			'}';
	}
}
