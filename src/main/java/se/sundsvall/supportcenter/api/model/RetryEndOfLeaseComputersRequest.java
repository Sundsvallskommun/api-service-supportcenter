package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;

@Schema(description = "RetryEndOfLeaseComputersRequest model")
public class RetryEndOfLeaseComputersRequest {

	@ArraySchema(schema = @Schema(examples = "J123ABC", description = "Serial number of a computer to put back in the queue"),
		arraySchema = @Schema(description = "The computers to put back in the queue. Left out, every computer the municipality has been given up on is taken"))
	// Capped because the serial numbers end up in one IN clause. A caller who sends their whole fleet gets a bad
	// request rather than a query the database has to be talked out of.
	@Size(max = 1000)
	private List<String> serialNumbers;

	public static RetryEndOfLeaseComputersRequest create() {
		return new RetryEndOfLeaseComputersRequest();
	}

	public List<String> getSerialNumbers() {
		return serialNumbers;
	}

	public void setSerialNumbers(List<String> serialNumbers) {
		this.serialNumbers = serialNumbers;
	}

	public RetryEndOfLeaseComputersRequest withSerialNumbers(List<String> serialNumbers) {
		this.serialNumbers = serialNumbers;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof final RetryEndOfLeaseComputersRequest that))
			return false;
		return Objects.equals(serialNumbers, that.serialNumbers);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(serialNumbers);
	}

	@Override
	public String toString() {
		return "RetryEndOfLeaseComputersRequest{" +
			"serialNumbers=" + serialNumbers +
			'}';
	}
}
