package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "EndOfLeaseComputer model")
public class EndOfLeaseComputer {

	@Schema(examples = "J123ABC", description = "Serial number", requiredMode = REQUIRED)
	@NotBlank(message = "must be provided")
	@Size(min = 1, max = 64)
	private String serialNumber;

	@Schema(examples = "WB16603", description = "Asset tag", requiredMode = REQUIRED)
	@NotBlank(message = "must be provided")
	@Size(min = 1, max = 64)
	private String assetTag;

	@Schema(examples = "2026-11-30", description = "Date when the lease ends. Stored for traceability, does not control when the message is sent", requiredMode = REQUIRED)
	@NotNull(message = "must be provided")
	private LocalDate endOfLeaseDate;

	public static EndOfLeaseComputer create() {
		return new EndOfLeaseComputer();
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public EndOfLeaseComputer withSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
		return this;
	}

	public String getAssetTag() {
		return assetTag;
	}

	public void setAssetTag(String assetTag) {
		this.assetTag = assetTag;
	}

	public EndOfLeaseComputer withAssetTag(String assetTag) {
		this.assetTag = assetTag;
		return this;
	}

	public LocalDate getEndOfLeaseDate() {
		return endOfLeaseDate;
	}

	public void setEndOfLeaseDate(LocalDate endOfLeaseDate) {
		this.endOfLeaseDate = endOfLeaseDate;
	}

	public EndOfLeaseComputer withEndOfLeaseDate(LocalDate endOfLeaseDate) {
		this.endOfLeaseDate = endOfLeaseDate;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof final EndOfLeaseComputer that))
			return false;
		return Objects.equals(serialNumber, that.serialNumber) && Objects.equals(assetTag, that.assetTag) && Objects.equals(endOfLeaseDate, that.endOfLeaseDate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(serialNumber, assetTag, endOfLeaseDate);
	}

	@Override
	public String toString() {
		return "EndOfLeaseComputer{" +
			"serialNumber='" + serialNumber + '\'' +
			", assetTag='" + assetTag + '\'' +
			", endOfLeaseDate=" + endOfLeaseDate +
			'}';
	}
}
