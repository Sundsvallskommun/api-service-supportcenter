package se.sundsvall.supportcenter.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.GenerationType.UUID;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "end_of_lease_batch",
	uniqueConstraints = @UniqueConstraint(
		name = "uq_end_of_lease_batch_municipality_id_external_batch_id",
		columnNames = {
			"municipality_id", "external_batch_id"
		}))
public class EndOfLeaseBatchEntity {

	@Id
	@GeneratedValue(strategy = UUID)
	@Column(name = "id")
	private String id;

	/**
	 * The id the sender gave the batch. Unique together with the municipality, which is what makes a resend recognizable
	 * as one without letting one sender's numbering reach another sender's batch.
	 */
	@Column(name = "external_batch_id", nullable = false, updatable = false, length = 36)
	private String externalBatchId;

	/**
	 * The municipality of the sender. Not the municipality of any computer in the batch, which is read from POB and kept
	 * on the computer.
	 */
	@Column(name = "municipality_id", nullable = false, updatable = false, length = 4)
	private String municipalityId;

	@Column(name = "created", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@OneToMany(mappedBy = "batch", cascade = ALL, orphanRemoval = true)
	private List<EndOfLeaseComputerEntity> computers;

	public static EndOfLeaseBatchEntity create() {
		return new EndOfLeaseBatchEntity();
	}

	@PrePersist
	void prePersist() {
		created = now(systemDefault());
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public EndOfLeaseBatchEntity withId(String id) {
		this.id = id;
		return this;
	}

	public String getExternalBatchId() {
		return externalBatchId;
	}

	public void setExternalBatchId(String externalBatchId) {
		this.externalBatchId = externalBatchId;
	}

	public EndOfLeaseBatchEntity withExternalBatchId(String externalBatchId) {
		this.externalBatchId = externalBatchId;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public EndOfLeaseBatchEntity withMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public EndOfLeaseBatchEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public List<EndOfLeaseComputerEntity> getComputers() {
		return computers;
	}

	public void setComputers(List<EndOfLeaseComputerEntity> computers) {
		this.computers = computers;
	}

	public EndOfLeaseBatchEntity withComputers(List<EndOfLeaseComputerEntity> computers) {
		this.computers = computers;
		return this;
	}

	/**
	 * The computers are left out of equals, hashCode and toString. The collection is lazy, so touching it here loads it
	 * behind the caller's back inside a session and throws outside one, which is not what a log line or an assertion is
	 * asking for.
	 */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof final EndOfLeaseBatchEntity that))
			return false;
		return Objects.equals(id, that.id) && Objects.equals(externalBatchId, that.externalBatchId) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(created, that.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, externalBatchId, municipalityId, created);
	}

	@Override
	public String toString() {
		return "EndOfLeaseBatchEntity{" +
			"id='" + id + '\'' +
			", externalBatchId='" + externalBatchId + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", created=" + created +
			'}';
	}
}
