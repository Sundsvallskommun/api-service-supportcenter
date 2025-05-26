package se.sundsvall.supportcenter.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Asset model")
public class Asset {

	@Schema(example = "123456", description = "Id of asset")
	private String id;

	@Schema(example = "4VVRN2", description = "Serial-number")
	private String serialNumber;

	@Schema(example = "WB12345NY", description = "Hardware-name")
	private String hardwareName;

	@Schema(example = "Computer", description = "Description of hardware")
	private String hardwareDescription;

	@Schema(example = "00:00:0a:bb:28:fc", description = "MAC address for the unit")
	private String macAddress;

	@Schema(description = "Supplier status")
	private String supplierStatus;

	@Schema(description = "Hardware status")
	private String hardwareStatus;

	@Schema(example = "x_dell", description = "Manufacturer")
	private String manufacturer;

	@Schema(example = "Latitude 9000", description = "Model name")
	private String modelName;

	@Schema(example = "Latitude 9000", description = "Description of model")
	private String modelDescription;

	@Schema(example = "2022-01-01", description = "Warranty end date")
	private LocalDate warrantyEndDate;

	@Schema(example = "2022-01-01", description = "Delivery date")
	private LocalDate deliveryDate;

	@Schema(example = "2281", description = "Municipality id")
	private String municipalityId;

	@Schema(example = "status1", description = "Lease status")
	private String leaseStatus;

	public static Asset create() {
		return new Asset();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Asset withId(String id) {
		this.id = id;
		return this;
	}

	public String getLeaseStatus() {
		return leaseStatus;
	}

	public void setLeaseStatus(String leaseStatus) {
		this.leaseStatus = leaseStatus;
	}

	public Asset withLeaseStatus(String leaseStatus) {
		this.leaseStatus = leaseStatus;
		return this;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public Asset withSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
		return this;
	}

	public String getHardwareName() {
		return hardwareName;
	}

	public void setHardwareName(String hardwareName) {
		this.hardwareName = hardwareName;
	}

	public Asset withHardwareName(String hardwareName) {
		this.hardwareName = hardwareName;
		return this;
	}

	public String getHardwareDescription() {
		return hardwareDescription;
	}

	public void setHardwareDescription(String hardwareDescription) {
		this.hardwareDescription = hardwareDescription;
	}

	public Asset withHardwareDescription(String hardwareDescription) {
		this.hardwareDescription = hardwareDescription;
		return this;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}

	public Asset withMacAddress(String macAddress) {
		this.macAddress = macAddress;
		return this;
	}

	public String getSupplierStatus() {
		return supplierStatus;
	}

	public void setSupplierStatus(String supplierStatus) {
		this.supplierStatus = supplierStatus;
	}

	public Asset withSupplierStatus(String supplierStatus) {
		this.supplierStatus = supplierStatus;
		return this;
	}

	public String getHardwareStatus() {
		return hardwareStatus;
	}

	public void setHardwareStatus(String hardwareStatus) {
		this.hardwareStatus = hardwareStatus;
	}

	public Asset withHardwareStatus(String hardwareStatus) {
		this.hardwareStatus = hardwareStatus;
		return this;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}

	public Asset withManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
		return this;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public Asset withModelName(String modelName) {
		this.modelName = modelName;
		return this;
	}

	public String getModelDescription() {
		return modelDescription;
	}

	public void setModelDescription(String modelDescription) {
		this.modelDescription = modelDescription;
	}

	public Asset withModelDescription(String modelDescription) {
		this.modelDescription = modelDescription;
		return this;
	}

	public LocalDate getWarrantyEndDate() {
		return warrantyEndDate;
	}

	public void setWarrantyEndDate(LocalDate warrantyEndDate) {
		this.warrantyEndDate = warrantyEndDate;
	}

	public Asset withWarrantyEndDate(LocalDate warrantyEndDate) {
		this.warrantyEndDate = warrantyEndDate;
		return this;
	}

	public LocalDate getDeliveryDate() {
		return deliveryDate;
	}

	public void setDeliveryDate(LocalDate deliveryDate) {
		this.deliveryDate = deliveryDate;
	}

	public Asset withDeliveryDate(LocalDate deliveryDate) {
		this.deliveryDate = deliveryDate;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public Asset withMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	@Override
	public String toString() {
		return "Asset{" +
			"id='" + id + '\'' +
			", serialNumber='" + serialNumber + '\'' +
			", hardwareName='" + hardwareName + '\'' +
			", hardwareDescription='" + hardwareDescription + '\'' +
			", macAddress='" + macAddress + '\'' +
			", supplierStatus='" + supplierStatus + '\'' +
			", hardwareStatus='" + hardwareStatus + '\'' +
			", manufacturer='" + manufacturer + '\'' +
			", modelName='" + modelName + '\'' +
			", modelDescription='" + modelDescription + '\'' +
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
		Asset asset = (Asset) o;
		return Objects.equals(id, asset.id) && Objects.equals(serialNumber, asset.serialNumber) && Objects.equals(hardwareName, asset.hardwareName) && Objects.equals(hardwareDescription, asset.hardwareDescription)
			&& Objects.equals(macAddress, asset.macAddress) && Objects.equals(supplierStatus, asset.supplierStatus) && Objects.equals(hardwareStatus, asset.hardwareStatus) && Objects.equals(manufacturer,
				asset.manufacturer) && Objects.equals(modelName, asset.modelName) && Objects.equals(modelDescription, asset.modelDescription) && Objects.equals(warrantyEndDate, asset.warrantyEndDate) && Objects.equals(
					deliveryDate, asset.deliveryDate) && Objects.equals(municipalityId, asset.municipalityId) && Objects.equals(leaseStatus, asset.leaseStatus);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, serialNumber, hardwareName, hardwareDescription, macAddress, supplierStatus, hardwareStatus, manufacturer, modelName, modelDescription, warrantyEndDate, deliveryDate, municipalityId, leaseStatus);
	}
}
