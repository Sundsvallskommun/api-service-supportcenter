package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Schema(description = "EndOfLeaseBatchStatusResponse model")
public class EndOfLeaseBatchStatusResponse {

	@Schema(examples = "8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c", description = "Id of the batch")
	private String id;

	@Schema(examples = "d1f3a8c2-9b7e-4a5f-8c3d-2e6b1a4f7c90", description = "Id the sender gave the batch")
	private String externalBatchId;

	@Schema(examples = "2026-09-17T06:03:11+02:00", description = "When the batch was registered")
	private OffsetDateTime created;

	@Schema(examples = "982", description = "Number of computers the batch was received with")
	private long total;

	@Schema(examples = "0", description = "Number of computers in the batch waiting to be sent")
	private long pending;

	@Schema(examples = "961", description = "Number of computers in the batch that have been sent their message")
	private long sent;

	@Schema(examples = "3", description = "Number of computers in the batch whose attempts are used up")
	private long failed;

	@Schema(examples = "18", description = "Number of computers in the batch that are never sent a message")
	private long excluded;

	@ArraySchema(schema = @Schema(description = "Every computer in the batch, in state order and then by serial number", implementation = EndOfLeaseComputerStatus.class))
	private List<EndOfLeaseComputerStatus> computers;

	public static EndOfLeaseBatchStatusResponse create() {
		return new EndOfLeaseBatchStatusResponse();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public EndOfLeaseBatchStatusResponse withId(String id) {
		this.id = id;
		return this;
	}

	public String getExternalBatchId() {
		return externalBatchId;
	}

	public void setExternalBatchId(String externalBatchId) {
		this.externalBatchId = externalBatchId;
	}

	public EndOfLeaseBatchStatusResponse withExternalBatchId(String externalBatchId) {
		this.externalBatchId = externalBatchId;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public EndOfLeaseBatchStatusResponse withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public EndOfLeaseBatchStatusResponse withTotal(long total) {
		this.total = total;
		return this;
	}

	public long getPending() {
		return pending;
	}

	public void setPending(long pending) {
		this.pending = pending;
	}

	public EndOfLeaseBatchStatusResponse withPending(long pending) {
		this.pending = pending;
		return this;
	}

	public long getSent() {
		return sent;
	}

	public void setSent(long sent) {
		this.sent = sent;
	}

	public EndOfLeaseBatchStatusResponse withSent(long sent) {
		this.sent = sent;
		return this;
	}

	public long getFailed() {
		return failed;
	}

	public void setFailed(long failed) {
		this.failed = failed;
	}

	public EndOfLeaseBatchStatusResponse withFailed(long failed) {
		this.failed = failed;
		return this;
	}

	public long getExcluded() {
		return excluded;
	}

	public void setExcluded(long excluded) {
		this.excluded = excluded;
	}

	public EndOfLeaseBatchStatusResponse withExcluded(long excluded) {
		this.excluded = excluded;
		return this;
	}

	public List<EndOfLeaseComputerStatus> getComputers() {
		return computers;
	}

	public void setComputers(List<EndOfLeaseComputerStatus> computers) {
		this.computers = computers;
	}

	public EndOfLeaseBatchStatusResponse withComputers(List<EndOfLeaseComputerStatus> computers) {
		this.computers = computers;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof final EndOfLeaseBatchStatusResponse that))
			return false;
		return total == that.total && pending == that.pending && sent == that.sent && failed == that.failed && excluded == that.excluded && Objects.equals(id, that.id) && Objects.equals(externalBatchId, that.externalBatchId)
			&& Objects.equals(created, that.created) && Objects.equals(computers, that.computers);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, externalBatchId, created, total, pending, sent, failed, excluded, computers);
	}

	@Override
	public String toString() {
		return "EndOfLeaseBatchStatusResponse{" +
			"id='" + id + '\'' +
			", externalBatchId='" + externalBatchId + '\'' +
			", created=" + created +
			", total=" + total +
			", pending=" + pending +
			", sent=" + sent +
			", failed=" + failed +
			", excluded=" + excluded +
			", computers=" + computers +
			'}';
	}
}
