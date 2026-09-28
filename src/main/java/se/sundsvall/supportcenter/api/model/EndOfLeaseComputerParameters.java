package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.OneOf;
import se.sundsvall.dept44.models.api.paging.AbstractParameterPagingBase;

/**
 * Which computers of a batch to read, and which page of them.
 *
 * The page and limit come from the base, which caps the limit at what
 * {@code dept44.models.api.paging.max.limit} is configured to. That cap is what stands between a batch of a thousand
 * and a single call that reads all of it.
 */
@Schema(description = "EndOfLeaseComputerParameters model")
public class EndOfLeaseComputerParameters extends AbstractParameterPagingBase {

	@ArraySchema(schema = @Schema(description = "States to list, one or more of PENDING, SENT, FAILED and EXCLUDED. Every state is listed when the parameter is left out", examples = "FAILED", type = "string"))
	private List<@OneOf({
		"PENDING", "SENT", "FAILED", "EXCLUDED"
	}) String> status;

	public static EndOfLeaseComputerParameters create() {
		return new EndOfLeaseComputerParameters();
	}

	public List<String> getStatus() {
		return status;
	}

	public void setStatus(List<String> status) {
		this.status = status;
	}

	public EndOfLeaseComputerParameters withStatus(List<String> status) {
		this.status = status;
		return this;
	}

	public EndOfLeaseComputerParameters withPage(int page) {
		setPage(page);
		return this;
	}

	public EndOfLeaseComputerParameters withLimit(int limit) {
		setLimit(limit);
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof final EndOfLeaseComputerParameters that))
			return false;
		return super.equals(o) && Objects.equals(status, that.status);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), status);
	}

	@Override
	public String toString() {
		return "EndOfLeaseComputerParameters{" +
			"page=" + getPage() +
			", limit=" + getLimit() +
			", status=" + status +
			'}';
	}
}
