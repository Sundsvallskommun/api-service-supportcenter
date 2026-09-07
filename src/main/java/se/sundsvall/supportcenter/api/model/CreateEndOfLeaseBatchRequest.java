package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "CreateEndOfLeaseBatchRequest model")
public class CreateEndOfLeaseBatchRequest {

	@ArraySchema(schema = @Schema(description = "Computers that have reached end of lease", requiredMode = REQUIRED, implementation = EndOfLeaseComputer.class))
	@NotEmpty(message = "must contain at least one computer")
	@Valid
	private List<EndOfLeaseComputer> computers;

	public static CreateEndOfLeaseBatchRequest create() {
		return new CreateEndOfLeaseBatchRequest();
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
		return Objects.equals(computers, that.computers);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(computers);
	}

	@Override
	public String toString() {
		return "CreateEndOfLeaseBatchRequest{" +
			"computers=" + computers +
			'}';
	}
}
