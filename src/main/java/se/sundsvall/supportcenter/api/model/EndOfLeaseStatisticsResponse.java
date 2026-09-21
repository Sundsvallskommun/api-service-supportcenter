package se.sundsvall.supportcenter.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.models.api.paging.PagingMetaData;

@Schema(description = "EndOfLeaseStatisticsResponse model")
public class EndOfLeaseStatisticsResponse {

	@Schema(examples = "2026-08-18", description = "First day the counts cover. The window that was asked for, or the month up to today when none was")
	private LocalDate from;

	@Schema(examples = "2026-09-18", description = "Last day the counts cover, counted in full")
	private LocalDate to;

	@Schema(description = "The computers of every batch in the window, counted per state. Covers the whole window, not the page of batches below")
	private EndOfLeaseComputerCounts counts;

	@ArraySchema(schema = @Schema(description = "One page of the batches in the window, with the same counts one batch at a time, newest batch first", implementation = EndOfLeaseBatchStatistics.class))
	private List<EndOfLeaseBatchStatistics> batches;

	@JsonProperty("_meta")
	@Schema(implementation = PagingMetaData.class, description = "The page of batches this answer holds. totalRecords is how many batches the municipality registered inside the window")
	private PagingMetaData metadata;

	public static EndOfLeaseStatisticsResponse create() {
		return new EndOfLeaseStatisticsResponse();
	}

	public LocalDate getFrom() {
		return from;
	}

	public void setFrom(LocalDate from) {
		this.from = from;
	}

	public EndOfLeaseStatisticsResponse withFrom(LocalDate from) {
		this.from = from;
		return this;
	}

	public LocalDate getTo() {
		return to;
	}

	public void setTo(LocalDate to) {
		this.to = to;
	}

	public EndOfLeaseStatisticsResponse withTo(LocalDate to) {
		this.to = to;
		return this;
	}

	public EndOfLeaseComputerCounts getCounts() {
		return counts;
	}

	public void setCounts(EndOfLeaseComputerCounts counts) {
		this.counts = counts;
	}

	public EndOfLeaseStatisticsResponse withCounts(EndOfLeaseComputerCounts counts) {
		this.counts = counts;
		return this;
	}

	public List<EndOfLeaseBatchStatistics> getBatches() {
		return batches;
	}

	public void setBatches(List<EndOfLeaseBatchStatistics> batches) {
		this.batches = batches;
	}

	public EndOfLeaseStatisticsResponse withBatches(List<EndOfLeaseBatchStatistics> batches) {
		this.batches = batches;
		return this;
	}

	public PagingMetaData getMetadata() {
		return metadata;
	}

	public void setMetadata(PagingMetaData metadata) {
		this.metadata = metadata;
	}

	public EndOfLeaseStatisticsResponse withMetadata(PagingMetaData metadata) {
		this.metadata = metadata;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof final EndOfLeaseStatisticsResponse that))
			return false;
		return Objects.equals(from, that.from) && Objects.equals(to, that.to) && Objects.equals(counts, that.counts) && Objects.equals(batches, that.batches) && Objects.equals(metadata, that.metadata);
	}

	@Override
	public int hashCode() {
		return Objects.hash(from, to, counts, batches, metadata);
	}

	@Override
	public String toString() {
		return "EndOfLeaseStatisticsResponse{" +
			"from=" + from +
			", to=" + to +
			", counts=" + counts +
			", batches=" + batches +
			", metadata=" + metadata +
			'}';
	}
}
