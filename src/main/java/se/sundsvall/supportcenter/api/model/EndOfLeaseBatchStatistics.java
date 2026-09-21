package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;

@Schema(description = "EndOfLeaseBatchStatistics model")
public class EndOfLeaseBatchStatistics {

	@Schema(examples = "8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c", description = "Id of the batch, which is what a single batch is read by")
	private String id;

	@Schema(examples = "d1f3a8c2-9b7e-4a5f-8c3d-2e6b1a4f7c90", description = "Id the sender gave the batch")
	private String externalBatchId;

	@Schema(examples = "2026-09-17T06:03:11+02:00", description = "When the batch was registered")
	private OffsetDateTime created;

	@Schema(description = "The computers of the batch, counted per state")
	private EndOfLeaseComputerCounts counts;

	public static EndOfLeaseBatchStatistics create() {
		return new EndOfLeaseBatchStatistics();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public EndOfLeaseBatchStatistics withId(String id) {
		this.id = id;
		return this;
	}

	public String getExternalBatchId() {
		return externalBatchId;
	}

	public void setExternalBatchId(String externalBatchId) {
		this.externalBatchId = externalBatchId;
	}

	public EndOfLeaseBatchStatistics withExternalBatchId(String externalBatchId) {
		this.externalBatchId = externalBatchId;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public EndOfLeaseBatchStatistics withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public EndOfLeaseComputerCounts getCounts() {
		return counts;
	}

	public void setCounts(EndOfLeaseComputerCounts counts) {
		this.counts = counts;
	}

	public EndOfLeaseBatchStatistics withCounts(EndOfLeaseComputerCounts counts) {
		this.counts = counts;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof final EndOfLeaseBatchStatistics that))
			return false;
		return Objects.equals(id, that.id) && Objects.equals(externalBatchId, that.externalBatchId) && Objects.equals(created, that.created) && Objects.equals(counts, that.counts);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, externalBatchId, created, counts);
	}

	@Override
	public String toString() {
		return "EndOfLeaseBatchStatistics{" +
			"id='" + id + '\'' +
			", externalBatchId='" + externalBatchId + '\'' +
			", created=" + created +
			", counts=" + counts +
			'}';
	}
}
