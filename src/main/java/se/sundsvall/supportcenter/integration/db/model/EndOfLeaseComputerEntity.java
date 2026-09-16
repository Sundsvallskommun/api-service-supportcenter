package se.sundsvall.supportcenter.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.UUID;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Optional.ofNullable;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "end_of_lease_computer", indexes = {
	@Index(name = "ix_end_of_lease_computer_batch_id", columnList = "batch_id"),
	@Index(name = "ix_end_of_lease_computer_status_created", columnList = "status, created"),
	@Index(name = "ix_end_of_lease_computer_serial_number", columnList = "serial_number"),
	@Index(name = "ix_end_of_lease_computer_asset_tag", columnList = "asset_tag")
})
public class EndOfLeaseComputerEntity {

	@Id
	@GeneratedValue(strategy = UUID)
	@Column(name = "id")
	private String id;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "batch_id", nullable = false, foreignKey = @ForeignKey(name = "fk_end_of_lease_computer_batch"))
	private EndOfLeaseBatchEntity batch;

	@Column(name = "serial_number", nullable = false, length = 64)
	private String serialNumber;

	@Column(name = "asset_tag", nullable = false, length = 64)
	private String assetTag;

	@Column(name = "end_of_lease_date", nullable = false)
	private LocalDate endOfLeaseDate;

	@Enumerated(STRING)
	@Column(name = "status", nullable = false, length = 32)
	private EndOfLeaseStatus status;

	/**
	 * The municipality on the POB configuration item (Virtual.CIKommun), which decides the SysMan instance the computer
	 * belongs to. Null until the computer has been picked up and looked up, and unrelated to the municipality the batch
	 * was sent to.
	 */
	@Column(name = "asset_municipality_id", length = 4)
	private String assetMunicipalityId;

	@Column(name = "attempts", nullable = false)
	private Integer attempts;

	/**
	 * The width of the error_message column, so that the cut in EndOfLeaseMapper.toErrorMessage and the varchar in the
	 * migration have one place to agree with rather than three literals that agree only by luck.
	 */
	public static final int ERROR_MESSAGE_LENGTH = 2048;

	@Column(name = "error_message", length = ERROR_MESSAGE_LENGTH)
	private String errorMessage;

	/**
	 * When the computer may be picked up again, or null for right away. Set by the dispatch run when SysMan could not
	 * be reached, so a group that costs no attempt cannot keep its place at the front of every page. Cleared as soon as
	 * a run gets through.
	 */
	@Column(name = "retry_after")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime retryAfter;

	@Column(name = "sent_at")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime sentAt;

	@Column(name = "created", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	/**
	 * Rows are read detached and written back with a merge of the whole entity. Two instances that both believe they
	 * hold the ShedLock lock would otherwise overwrite each other without a sound; with this the second one fails.
	 */
	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	public static EndOfLeaseComputerEntity create() {
		return new EndOfLeaseComputerEntity();
	}

	@PrePersist
	void prePersist() {
		created = ofNullable(created).orElse(now(systemDefault()));
		attempts = ofNullable(attempts).orElse(0);
	}

	@PreUpdate
	void preUpdate() {
		modified = now(systemDefault());
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public EndOfLeaseComputerEntity withId(String id) {
		this.id = id;
		return this;
	}

	public EndOfLeaseBatchEntity getBatch() {
		return batch;
	}

	public void setBatch(EndOfLeaseBatchEntity batch) {
		this.batch = batch;
	}

	public EndOfLeaseComputerEntity withBatch(EndOfLeaseBatchEntity batch) {
		this.batch = batch;
		return this;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public EndOfLeaseComputerEntity withSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
		return this;
	}

	public String getAssetTag() {
		return assetTag;
	}

	public void setAssetTag(String assetTag) {
		this.assetTag = assetTag;
	}

	public EndOfLeaseComputerEntity withAssetTag(String assetTag) {
		this.assetTag = assetTag;
		return this;
	}

	public LocalDate getEndOfLeaseDate() {
		return endOfLeaseDate;
	}

	public void setEndOfLeaseDate(LocalDate endOfLeaseDate) {
		this.endOfLeaseDate = endOfLeaseDate;
	}

	public EndOfLeaseComputerEntity withEndOfLeaseDate(LocalDate endOfLeaseDate) {
		this.endOfLeaseDate = endOfLeaseDate;
		return this;
	}

	public EndOfLeaseStatus getStatus() {
		return status;
	}

	public void setStatus(EndOfLeaseStatus status) {
		this.status = status;
	}

	public EndOfLeaseComputerEntity withStatus(EndOfLeaseStatus status) {
		this.status = status;
		return this;
	}

	public String getAssetMunicipalityId() {
		return assetMunicipalityId;
	}

	public void setAssetMunicipalityId(String assetMunicipalityId) {
		this.assetMunicipalityId = assetMunicipalityId;
	}

	public EndOfLeaseComputerEntity withAssetMunicipalityId(String assetMunicipalityId) {
		this.assetMunicipalityId = assetMunicipalityId;
		return this;
	}

	public Integer getAttempts() {
		return attempts;
	}

	public void setAttempts(Integer attempts) {
		this.attempts = attempts;
	}

	public EndOfLeaseComputerEntity withAttempts(Integer attempts) {
		this.attempts = attempts;
		return this;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public EndOfLeaseComputerEntity withErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
		return this;
	}

	public OffsetDateTime getRetryAfter() {
		return retryAfter;
	}

	public void setRetryAfter(OffsetDateTime retryAfter) {
		this.retryAfter = retryAfter;
	}

	public EndOfLeaseComputerEntity withRetryAfter(OffsetDateTime retryAfter) {
		this.retryAfter = retryAfter;
		return this;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public EndOfLeaseComputerEntity withVersion(Long version) {
		this.version = version;
		return this;
	}

	public OffsetDateTime getSentAt() {
		return sentAt;
	}

	public void setSentAt(OffsetDateTime sentAt) {
		this.sentAt = sentAt;
	}

	public EndOfLeaseComputerEntity withSentAt(OffsetDateTime sentAt) {
		this.sentAt = sentAt;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public EndOfLeaseComputerEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	public EndOfLeaseComputerEntity withModified(OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	/**
	 * The batch is left out of equals, hashCode and toString. It is the other end of the relation the batch already
	 * holds, and following it back here makes all three recurse.
	 */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof final EndOfLeaseComputerEntity that))
			return false;
		return Objects.equals(id, that.id) && Objects.equals(serialNumber, that.serialNumber) && Objects.equals(assetTag, that.assetTag) && Objects.equals(endOfLeaseDate, that.endOfLeaseDate) && status == that.status && Objects.equals(
			assetMunicipalityId, that.assetMunicipalityId) && Objects.equals(attempts, that.attempts) && Objects.equals(errorMessage, that.errorMessage) && Objects.equals(retryAfter, that.retryAfter) && Objects.equals(sentAt, that.sentAt)
			&& Objects.equals(created, that.created) && Objects.equals(modified, that.modified) && Objects.equals(version, that.version);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, serialNumber, assetTag, endOfLeaseDate, status, assetMunicipalityId, attempts, errorMessage, retryAfter, sentAt, created, modified, version);
	}

	@Override
	public String toString() {
		return "EndOfLeaseComputerEntity{" +
			"id='" + id + '\'' +
			", serialNumber='" + serialNumber + '\'' +
			", assetTag='" + assetTag + '\'' +
			", endOfLeaseDate=" + endOfLeaseDate +
			", status=" + status +
			", assetMunicipalityId='" + assetMunicipalityId + '\'' +
			", attempts=" + attempts +
			", errorMessage='" + errorMessage + '\'' +
			", retryAfter=" + retryAfter +
			", sentAt=" + sentAt +
			", created=" + created +
			", modified=" + modified +
			", version=" + version +
			'}';
	}
}
