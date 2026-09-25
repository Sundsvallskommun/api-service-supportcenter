package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;

@Schema(description = "EndOfLeaseComputerStatus model")
public class EndOfLeaseComputerStatus {

	@Schema(examples = "J123ABC", description = "Serial number")
	private String serialNumber;

	@Schema(examples = "AB12345", description = "Asset tag, which is the name the computer is known by in SysMan")
	private String assetTag;

	@Schema(examples = "FAILED",
		description = "The state the computer is in: PENDING, SENT, FAILED or EXCLUDED. A string rather than an enumeration")
	private String status;

	@Schema(examples = "5", description = "How many attempts the computer has cost. An attempt is only spent on something that is the computer's own fault, so a dependency that could not be reached does not show here")
	private int attempts;

	@Schema(examples = "POB answered the serial number with no configuration item holding a municipality we can route on",
		description = "Why the last attempt did not succeed. Cleared as soon as an attempt gets through, so a computer that is SENT carries none even when it took several tries to get there")
	private String errorMessage;

	@Schema(examples = "2026-09-17T06:14:52+02:00", description = "When the message was sent, or null when it has not been")
	private OffsetDateTime sentAt;

	public static EndOfLeaseComputerStatus create() {
		return new EndOfLeaseComputerStatus();
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public EndOfLeaseComputerStatus withSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
		return this;
	}

	public String getAssetTag() {
		return assetTag;
	}

	public void setAssetTag(String assetTag) {
		this.assetTag = assetTag;
	}

	public EndOfLeaseComputerStatus withAssetTag(String assetTag) {
		this.assetTag = assetTag;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public EndOfLeaseComputerStatus withStatus(String status) {
		this.status = status;
		return this;
	}

	public int getAttempts() {
		return attempts;
	}

	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}

	public EndOfLeaseComputerStatus withAttempts(int attempts) {
		this.attempts = attempts;
		return this;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public EndOfLeaseComputerStatus withErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
		return this;
	}

	public OffsetDateTime getSentAt() {
		return sentAt;
	}

	public void setSentAt(OffsetDateTime sentAt) {
		this.sentAt = sentAt;
	}

	public EndOfLeaseComputerStatus withSentAt(OffsetDateTime sentAt) {
		this.sentAt = sentAt;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof final EndOfLeaseComputerStatus that))
			return false;
		return attempts == that.attempts && Objects.equals(serialNumber, that.serialNumber) && Objects.equals(assetTag, that.assetTag) && Objects.equals(status, that.status) && Objects.equals(errorMessage, that.errorMessage) && Objects.equals(sentAt,
			that.sentAt);
	}

	@Override
	public int hashCode() {
		return Objects.hash(serialNumber, assetTag, status, attempts, errorMessage, sentAt);
	}

	@Override
	public String toString() {
		return "EndOfLeaseComputerStatus{" +
			"serialNumber='" + serialNumber + '\'' +
			", assetTag='" + assetTag + '\'' +
			", status='" + status + '\'' +
			", attempts=" + attempts +
			", errorMessage='" + errorMessage + '\'' +
			", sentAt=" + sentAt +
			'}';
	}
}
