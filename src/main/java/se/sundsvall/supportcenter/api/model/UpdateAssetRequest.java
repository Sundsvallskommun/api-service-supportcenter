package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import se.sundsvall.supportcenter.api.validation.ValidMacAddress;

import java.time.LocalDate;
import java.util.Objects;

@Schema(description = "UpdateAssetRequest model")
public class UpdateAssetRequest {

	@Valid
	private Note note;

	@Schema(example = "Latitude 9000", description = "Hardware name")
	private String hardwareName;

	@Schema(example = "00:00:0a:bb:28:fc", description = "MAC address for the unit")
	@ValidMacAddress
	private String macAddress;

	@Schema(example = "0", description = "Supplier status")
	private String supplierStatus;

	@Schema(example = "0", description = "Hardware status")
	private String hardwareStatus;

	@Schema(example = "2022-01-01", description = "Warranty end date")
	private LocalDate warrantyEndDate;

	@Schema(example = "2022-01-01", description = "Delivery date")
	private LocalDate deliveryDate;

	@Schema(example = "2281", description = "Municipality id", hidden = true)
	private String municipalityId;

	public static UpdateAssetRequest create() {
		return new UpdateAssetRequest();
	}

	public Note getNote() {
		return note;
	}

	public void setNote(Note note) {
		this.note = note;
	}

	public UpdateAssetRequest withNote(Note note) {
		this.note = note;
		return this;
	}

	public String getHardwareName() {
		return hardwareName;
	}

	public void setHardwareName(String hardwareName) {
		this.hardwareName = hardwareName;
	}

	public UpdateAssetRequest withHardwareName(String hardwareName) {
		this.hardwareName = hardwareName;
		return this;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}

	public UpdateAssetRequest withMacAddress(String macAddress) {
		this.macAddress = macAddress;
		return this;
	}

	public String getSupplierStatus() {
		return supplierStatus;
	}

	public void setSupplierStatus(String supplierStatus) {
		this.supplierStatus = supplierStatus;
	}

	public UpdateAssetRequest withSupplierStatus(String supplierStatus) {
		this.supplierStatus = supplierStatus;
		return this;
	}

	public String getHardwareStatus() {
		return hardwareStatus;
	}

	public void setHardwareStatus(String hardwareStatus) {
		this.hardwareStatus = hardwareStatus;
	}

	public UpdateAssetRequest withHardwareStatus(String hardwareStatus) {
		this.hardwareStatus = hardwareStatus;
		return this;
	}

	public LocalDate getWarrantyEndDate() {
		return warrantyEndDate;
	}

	public void setWarrantyEndDate(LocalDate warrantyEndDate) {
		this.warrantyEndDate = warrantyEndDate;
	}

	public UpdateAssetRequest withWarrantyEndDate(LocalDate warrantyEndDate) {
		this.warrantyEndDate = warrantyEndDate;
		return this;
	}

	public LocalDate getDeliveryDate() {
		return deliveryDate;
	}

	public void setDeliveryDate(LocalDate deliveryDate) {
		this.deliveryDate = deliveryDate;
	}

	public UpdateAssetRequest withDeliveryDate(LocalDate deliveryDate) {
		this.deliveryDate = deliveryDate;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public UpdateAssetRequest withMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(hardwareName, macAddress, note, supplierStatus, hardwareStatus, warrantyEndDate, deliveryDate, municipalityId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UpdateAssetRequest other = (UpdateAssetRequest) obj;
		return Objects.equals(hardwareName, other.hardwareName) && Objects.equals(macAddress, other.macAddress)
			&& Objects.equals(note, other.note) && Objects.equals(supplierStatus, other.supplierStatus)
			&& Objects.equals(hardwareStatus, other.hardwareStatus) && Objects.equals(warrantyEndDate, other.warrantyEndDate)
			&& Objects.equals(deliveryDate, other.deliveryDate) && Objects.equals(municipalityId, other.municipalityId);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UpdateAssetRequest [note=").append(note)
			.append(", hardwareName=").append(hardwareName).append(", macAddress=").append(macAddress).append(", supplierStatus=")
			.append(supplierStatus).append(", hardwareStatus=").append(hardwareStatus).append(", warrantyEndDate=")
			.append(warrantyEndDate).append(", deliveryDate=").append(deliveryDate).append(", municipalityId=").append(municipalityId)
			.append("]");
		return builder.toString();
	}
}
