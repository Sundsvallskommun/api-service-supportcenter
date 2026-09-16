package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "RetryEndOfLeaseComputersResponse model")
public class RetryEndOfLeaseComputersResponse {

	@Schema(examples = "3", description = "Number of computers that were put back in the queue")
	private int reset;

	public static RetryEndOfLeaseComputersResponse create() {
		return new RetryEndOfLeaseComputersResponse();
	}

	public int getReset() {
		return reset;
	}

	public void setReset(int reset) {
		this.reset = reset;
	}

	public RetryEndOfLeaseComputersResponse withReset(int reset) {
		this.reset = reset;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof final RetryEndOfLeaseComputersResponse that))
			return false;
		return reset == that.reset;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(reset);
	}

	@Override
	public String toString() {
		return "RetryEndOfLeaseComputersResponse{" +
			"reset=" + reset +
			'}';
	}
}
