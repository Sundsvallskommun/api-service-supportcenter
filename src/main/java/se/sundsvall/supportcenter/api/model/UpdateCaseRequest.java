package se.sundsvall.supportcenter.api.model;

import java.util.Objects;

import javax.validation.Valid;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "UpdateCaseRequest model")
public class UpdateCaseRequest {

	@Valid
	private Note note;

	@Schema(description = "External case-ID", example = "INC232323")
	private String externalCaseId;

	@Schema(description = "Case status", example = "In Process")
	private String caseStatus;

	@Schema(description = "Case category", example = "IT Användarhantering")
	private String caseCategory;

	@Schema(description = "Closure code", example = "IT Byte/uppgradering av hårdvara")
	private String closureCode;

	@Schema(description = "Hardware name")
	private String hardwareName;

	@Schema(description = "Responsible group", example = "First Line IT")
	private String responsibleGroup;

	@Schema(description = "Serial number", example = "FRGDZ1J")
	private String serialNumber;

	public static UpdateCaseRequest create() {
		return new UpdateCaseRequest();
	}

	public Note getNote() {
		return note;
	}

	public void setNote(Note note) {
		this.note = note;
	}

	public UpdateCaseRequest withNote(Note note) {
		this.note = note;
		return this;
	}

	public String getExternalCaseId() {
		return externalCaseId;
	}

	public void setExternalCaseId(String externalCaseId) {
		this.externalCaseId = externalCaseId;
	}

	public UpdateCaseRequest withExternalCaseId(String externalCaseId) {
		this.externalCaseId = externalCaseId;
		return this;
	}

	public String getCaseStatus() {
		return caseStatus;
	}

	public void setCaseStatus(String caseStatus) {
		this.caseStatus = caseStatus;
	}

	public UpdateCaseRequest withCaseStatus(String caseStatus) {
		this.caseStatus = caseStatus;
		return this;
	}

	public String getCaseCategory() {
		return caseCategory;
	}

	public void setCaseCategory(String caseCategory) {
		this.caseCategory = caseCategory;
	}

	public UpdateCaseRequest withCaseCategory(String caseCategory) {
		this.caseCategory = caseCategory;
		return this;
	}

	public String getClosureCode() {
		return closureCode;
	}

	public void setClosureCode(String closureCode) {
		this.closureCode = closureCode;
	}

	public UpdateCaseRequest withClosureCode(String closureCode) {
		this.closureCode = closureCode;
		return this;
	}

	public String getHardwareName() {
		return hardwareName;
	}

	public void setHardwareName(String hardwareName) {
		this.hardwareName = hardwareName;
	}

	public UpdateCaseRequest withHardwareName(String hardwareName) {
		this.hardwareName = hardwareName;
		return this;
	}

	public String getResponsibleGroup() {
		return responsibleGroup;
	}

	public void setResponsibleGroup(String responsibleGroup) {
		this.responsibleGroup = responsibleGroup;
	}

	public UpdateCaseRequest withResponsibleGroup(String responsibleGroup) {
		this.responsibleGroup = responsibleGroup;
		return this;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public UpdateCaseRequest withSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
		return this;
	}

	@Override
	public int hashCode() { return Objects.hash(caseCategory, caseStatus, closureCode, externalCaseId, hardwareName, note, responsibleGroup, serialNumber); }

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UpdateCaseRequest other = (UpdateCaseRequest) obj;
		return Objects.equals(caseCategory, other.caseCategory) && Objects.equals(caseStatus, other.caseStatus) && Objects.equals(closureCode, other.closureCode) && Objects.equals(
			externalCaseId, other.externalCaseId) && Objects.equals(hardwareName, other.hardwareName) && Objects.equals(note, other.note) && Objects.equals(
				responsibleGroup, other.responsibleGroup) && Objects.equals(serialNumber, other.serialNumber);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UpdateCaseRequest [note=").append(note).append(", externalCaseId=").append(externalCaseId).append(", caseStatus=").append(caseStatus).append(
			", caseCategory=").append(caseCategory).append(", closureCode=").append(closureCode).append(", hardwareName=").append(hardwareName).append(
				", responsibleGroup=").append(responsibleGroup).append(", serialNumber=").append(serialNumber).append("]");
		return builder.toString();
	}
}
