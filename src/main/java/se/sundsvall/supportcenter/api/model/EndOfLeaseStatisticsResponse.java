package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Schema(description = "EndOfLeaseStatisticsResponse model")
public class EndOfLeaseStatisticsResponse {

	@Schema(examples = "2026-08-18", description = "First day the counts cover. The window that was asked for, or the month up to today when none was")
	private LocalDate from;

	@Schema(examples = "2026-09-18", description = "Last day the counts cover, counted in full")
	private LocalDate to;

	@Schema(examples = "128", description = "Number of batches the municipality registered inside the window")
	private int batches;

	@Schema(description = "The computers of every batch, counted per state")
	private EndOfLeaseComputerCounts computers;

	@ArraySchema(schema = @Schema(description = "The same counts for one batch at a time, newest batch first", implementation = EndOfLeaseBatchStatistics.class))
	private List<EndOfLeaseBatchStatistics> perBatch;

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

	public int getBatches() {
		return batches;
	}

	public void setBatches(int batches) {
		this.batches = batches;
	}

	public EndOfLeaseStatisticsResponse withBatches(int batches) {
		this.batches = batches;
		return this;
	}

	public EndOfLeaseComputerCounts getComputers() {
		return computers;
	}

	public void setComputers(EndOfLeaseComputerCounts computers) {
		this.computers = computers;
	}

	public EndOfLeaseStatisticsResponse withComputers(EndOfLeaseComputerCounts computers) {
		this.computers = computers;
		return this;
	}

	public List<EndOfLeaseBatchStatistics> getPerBatch() {
		return perBatch;
	}

	public void setPerBatch(List<EndOfLeaseBatchStatistics> perBatch) {
		this.perBatch = perBatch;
	}

	public EndOfLeaseStatisticsResponse withPerBatch(List<EndOfLeaseBatchStatistics> perBatch) {
		this.perBatch = perBatch;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof final EndOfLeaseStatisticsResponse that))
			return false;
		return batches == that.batches && Objects.equals(from, that.from) && Objects.equals(to, that.to) && Objects.equals(computers, that.computers) && Objects.equals(perBatch, that.perBatch);
	}

	@Override
	public int hashCode() {
		return Objects.hash(from, to, batches, computers, perBatch);
	}

	@Override
	public String toString() {
		return "EndOfLeaseStatisticsResponse{" +
			"from=" + from +
			", to=" + to +
			", batches=" + batches +
			", computers=" + computers +
			", perBatch=" + perBatch +
			'}';
	}
}
