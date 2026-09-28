package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "EndOfLeaseComputerCounts model")
public class EndOfLeaseComputerCounts {

	@Schema(examples = "1240", description = "Number of computers that have been received, which is the sum of the states below")
	private long total;

	@Schema(examples = "12", description = "Number of computers waiting to be sent, or waiting to be sent again after an attempt that did not succeed")
	private long pending;

	@Schema(examples = "1180", description = "Number of computers that have been sent their message")
	private long sent;

	@Schema(examples = "8", description = "Number of computers whose attempts are used up, which need to be looked at by a human")
	private long failed;

	@Schema(examples = "40", description = "Number of computers that are never sent a message, recognized by the letters their asset tag starts with")
	private long excluded;

	public static EndOfLeaseComputerCounts create() {
		return new EndOfLeaseComputerCounts();
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public EndOfLeaseComputerCounts withTotal(long total) {
		this.total = total;
		return this;
	}

	public long getPending() {
		return pending;
	}

	public void setPending(long pending) {
		this.pending = pending;
	}

	public EndOfLeaseComputerCounts withPending(long pending) {
		this.pending = pending;
		return this;
	}

	public long getSent() {
		return sent;
	}

	public void setSent(long sent) {
		this.sent = sent;
	}

	public EndOfLeaseComputerCounts withSent(long sent) {
		this.sent = sent;
		return this;
	}

	public long getFailed() {
		return failed;
	}

	public void setFailed(long failed) {
		this.failed = failed;
	}

	public EndOfLeaseComputerCounts withFailed(long failed) {
		this.failed = failed;
		return this;
	}

	public long getExcluded() {
		return excluded;
	}

	public void setExcluded(long excluded) {
		this.excluded = excluded;
	}

	public EndOfLeaseComputerCounts withExcluded(long excluded) {
		this.excluded = excluded;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof final EndOfLeaseComputerCounts that))
			return false;
		return total == that.total && pending == that.pending && sent == that.sent && failed == that.failed && excluded == that.excluded;
	}

	@Override
	public int hashCode() {
		return Objects.hash(total, pending, sent, failed, excluded);
	}

	@Override
	public String toString() {
		return "EndOfLeaseComputerCounts{" +
			"total=" + total +
			", pending=" + pending +
			", sent=" + sent +
			", failed=" + failed +
			", excluded=" + excluded +
			'}';
	}
}
