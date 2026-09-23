package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "CreateEndOfLeaseBatchRequest model")
public class CreateEndOfLeaseBatchRequest {

	@Schema(examples = "DSET0001234", description = "Id of the batch, set by the sender. A batch sent again under an id that is already registered is rejected with 409 Conflict and not queued a second time", requiredMode = REQUIRED)
	@Size(min = 1, max = 36)
	@NotBlank(message = "must be provided")
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
