package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import se.sundsvall.dept44.models.api.paging.AbstractParameterPagingBase;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

/**
 * Which window to count, and which page of the batches inside it to answer with.
 *
 * The page and limit come from the base, which caps the limit at what
 * {@code dept44.models.api.paging.max.limit} is configured to. The window no longer decides how large the answer is,
 * only how many rows the database counts over.
 */
@Schema(description = "EndOfLeaseStatisticsParameters model")
public class EndOfLeaseStatisticsParameters extends AbstractParameterPagingBase {

	@Schema(examples = "2026-08-18", description = "First day to count, by the day the batch was registered. A month back from the last day when left out")
	@DateTimeFormat(iso = DATE)
	private LocalDate from;

	@Schema(examples = "2026-09-18", description = "Last day to count, counted in full. Today when left out")
	@DateTimeFormat(iso = DATE)
	private LocalDate to;

	public static EndOfLeaseStatisticsParameters create() {
		return new EndOfLeaseStatisticsParameters();
	}

	public LocalDate getFrom() {
		return from;
	}

	public void setFrom(LocalDate from) {
		this.from = from;
	}

	public EndOfLeaseStatisticsParameters withFrom(LocalDate from) {
		this.from = from;
		return this;
	}

	public LocalDate getTo() {
		return to;
	}

	public void setTo(LocalDate to) {
		this.to = to;
	}

	public EndOfLeaseStatisticsParameters withTo(LocalDate to) {
		this.to = to;
		return this;
	}

	public EndOfLeaseStatisticsParameters withPage(int page) {
		setPage(page);
		return this;
	}

	public EndOfLeaseStatisticsParameters withLimit(int limit) {
		setLimit(limit);
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof final EndOfLeaseStatisticsParameters that))
			return false;
		return super.equals(o) && Objects.equals(from, that.from) && Objects.equals(to, that.to);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), from, to);
	}

	@Override
	public String toString() {
		return "EndOfLeaseStatisticsParameters{" +
			"page=" + getPage() +
			", limit=" + getLimit() +
			", from=" + from +
			", to=" + to +
			'}';
	}
}
