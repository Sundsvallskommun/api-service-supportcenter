package se.sundsvall.supportcenter.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.models.api.paging.PagingMetaData;

@Schema(description = "EndOfLeaseBatchStatusResponse model")
public class EndOfLeaseBatchStatusResponse {

	@Schema(examples = "8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c", description = "Id of the batch")
	private String id;

	@Schema(examples = "d1f3a8c2-9b7e-4a5f-8c3d-2e6b1a4f7c90", description = "Id the sender gave the batch")
	private String externalBatchId;

	@Schema(examples = "2026-09-17T06:03:11+02:00", description = "When the batch was registered")
	private OffsetDateTime created;

	@Schema(description = "The computers of the batch, counted per state. Covers the whole batch whichever states were asked for")
	private EndOfLeaseComputerCounts counts;

	@ArraySchema(schema = @Schema(description = "One page of the computers of the batch, the states that were asked for, in state order and then by serial number", implementation = EndOfLeaseComputerStatus.class))
	private List<EndOfLeaseComputerStatus> computers;

	@JsonProperty("_meta")
	@Schema(implementation = PagingMetaData.class, description = "The page of computers this answer holds. Counts the computers in the states that were asked for, not the whole batch")
	private PagingMetaData metadata;

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

	public EndOfLeaseComputerCounts getCounts() {
		return counts;
	}

	public void setCounts(EndOfLeaseComputerCounts counts) {
		this.counts = counts;
	}

	public EndOfLeaseBatchStatusResponse withCounts(EndOfLeaseComputerCounts counts) {
		this.counts = counts;
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

	public PagingMetaData getMetadata() {
		return metadata;
	}

	public void setMetadata(PagingMetaData metadata) {
		this.metadata = metadata;
	}

	public EndOfLeaseBatchStatusResponse withMetadata(PagingMetaData metadata) {
		this.metadata = metadata;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof final EndOfLeaseBatchStatusResponse that))
			return false;
		return Objects.equals(id, that.id) && Objects.equals(externalBatchId, that.externalBatchId) && Objects.equals(created, that.created) && Objects.equals(counts, that.counts) && Objects.equals(computers, that.computers)
			&& Objects.equals(metadata, that.metadata);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, externalBatchId, created, counts, computers, metadata);
	}

	@Override
	public String toString() {
		return "EndOfLeaseBatchStatusResponse{" +
			"id='" + id + '\'' +
			", externalBatchId='" + externalBatchId + '\'' +
			", created=" + created +
			", counts=" + counts +
			", computers=" + computers +
			", metadata=" + metadata +
			'}';
	}
}
