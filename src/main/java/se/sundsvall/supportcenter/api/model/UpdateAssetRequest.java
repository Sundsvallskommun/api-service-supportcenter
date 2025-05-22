package se.sundsvall.supportcenter.api.model;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Objects;
import se.sundsvall.supportcenter.api.validation.ValidMacAddress;

@Schema(description = "UpdateAssetRequest model")
public class UpdateAssetRequest {

	@Valid
	private Note note;

	@Schema(example = "Latitude 9000", description = "Hardware name", requiredMode = NOT_REQUIRED)
	private String hardwareName;

	@Schema(example = "00:00:0a:bb:28:fc", description = "MAC address for the unit", requiredMode = NOT_REQUIRED)
	@ValidMacAddress
	private String macAddress;

	@Schema(example = "0", description = "Supplier status", requiredMode = NOT_REQUIRED)
	private String supplierStatus;

	@Schema(example = "0", description = "Hardware status", requiredMode = NOT_REQUIRED)
	private String hardwareStatus;

	@Schema(example = "2022-01-01", description = "Warranty end date", requiredMode = NOT_REQUIRED)
	private LocalDate warrantyEndDate;

	@Schema(example = "2022-01-01", description = "Delivery date", requiredMode = NOT_REQUIRED)
	private LocalDate deliveryDate;

	@Schema(example = "2281", description = "Municipality id", hidden = true, requiredMode = NOT_REQUIRED)
	private String municipalityId;

	@Schema(example = "status1", description = "Lease status", requiredMode = NOT_REQUIRED)
	private String leaseStatus;

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

	public String getLeaseStatus() {
		return leaseStatus;
	}

	public void setLeaseStatus(String leaseStatus) {
		this.leaseStatus = leaseStatus;
	}

	public UpdateAssetRequest withLeaseStatus(String leaseStatus) {
		this.leaseStatus = leaseStatus;
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
	public String toString() {
		return "UpdateAssetRequest{" +
			"note=" + note +
			", hardwareName='" + hardwareName + '\'' +
			", macAddress='" + macAddress + '\'' +
			", supplierStatus='" + supplierStatus + '\'' +
			", hardwareStatus='" + hardwareStatus + '\'' +
			", warrantyEndDate=" + warrantyEndDate +
			", deliveryDate=" + deliveryDate +
			", municipalityId='" + municipalityId + '\'' +
			", leaseStatus='" + leaseStatus + '\'' +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		UpdateAssetRequest that = (UpdateAssetRequest) o;
		return Objects.equals(note, that.note) && Objects.equals(hardwareName, that.hardwareName) && Objects.equals(macAddress, that.macAddress) && Objects.equals(supplierStatus, that.supplierStatus)
			&& Objects.equals(hardwareStatus, that.hardwareStatus) && Objects.equals(warrantyEndDate, that.warrantyEndDate) && Objects.equals(deliveryDate, that.deliveryDate) && Objects.equals(municipalityId,
				that.municipalityId) && Objects.equals(leaseStatus, that.leaseStatus);
	}

	@Override
	public int hashCode() {
		return Objects.hash(note, hardwareName, macAddress, supplierStatus, hardwareStatus, warrantyEndDate, deliveryDate, municipalityId, leaseStatus);
	}
}
