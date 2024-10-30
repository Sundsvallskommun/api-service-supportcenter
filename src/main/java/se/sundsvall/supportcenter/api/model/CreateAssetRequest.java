package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import se.sundsvall.supportcenter.api.validation.ValidMacAddress;

import java.time.LocalDate;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "CreateAssetRequest model")
public class CreateAssetRequest {

	@Schema(example = "x_dell", description = "Manufacturer")
	private String manufacturer;

	@Schema(example = "Latitude 9000", description = "Model name", requiredMode = REQUIRED)
	@NotBlank(message = "must be provided")
	private String modelName;

	@Schema(example = "Latitude 9000", description = "Description of asset")
	private String modelDescription;

	@Schema(example = "CD23456", description = "Serial number", requiredMode = REQUIRED)
	@NotBlank(message = "must be provided")
	private String serialNumber;

	@Schema(example = "00:00:0a:bb:28:fc", description = "MAC address for the unit")
	@ValidMacAddress
	private String macAddress;

	@Schema(example = "2022-01-01", description = "Warranty end date")
	private LocalDate warrantyEndDate;

	@Schema(example = "0", description = "Supplier status")
	private String supplierStatus;

	@Schema(example = "0", description = "Hardware status")
	private String hardwareStatus;

	@Schema(example = "2022-01-01", description = "Delivery date")
	private LocalDate deliveryDate;

	@Schema(example = "2281", description = "Municipality id", hidden = true)
	private String municipalityId;

	public static CreateAssetRequest create() {
		return new CreateAssetRequest();
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}

	public CreateAssetRequest withManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
		return this;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public CreateAssetRequest withModelName(String modelName) {
		this.modelName = modelName;
		return this;
	}

	public String getModelDescription() {
		return modelDescription;
	}

	public void setModelDescription(String modelDescription) {
		this.modelDescription = modelDescription;
	}

	public CreateAssetRequest withModelDescription(String modelDescription) {
		this.modelDescription = modelDescription;
		return this;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public CreateAssetRequest withSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
		return this;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}

	public CreateAssetRequest withMacAddress(String macAddress) {
		this.macAddress = macAddress;
		return this;
	}

	public LocalDate getWarrantyEndDate() {
		return warrantyEndDate;
	}

	public void setWarrantyEndDate(LocalDate warrantyEndDate) {
		this.warrantyEndDate = warrantyEndDate;
	}

	public CreateAssetRequest withWarrantyEndDate(LocalDate warrantyEndDate) {
		this.warrantyEndDate = warrantyEndDate;
		return this;
	}

	public String getSupplierStatus() {
		return supplierStatus;
	}

	public void setSupplierStatus(String supplierStatus) {
		this.supplierStatus = supplierStatus;
	}

	public CreateAssetRequest withSupplierStatus(String supplierStatus) {
		this.supplierStatus = supplierStatus;
		return this;
	}

	public String getHardwareStatus() {
		return hardwareStatus;
	}

	public void setHardwareStatus(String hardwareStatus) {
		this.hardwareStatus = hardwareStatus;
	}

	public CreateAssetRequest withHardwareStatus(String hardwareStatus) {
		this.hardwareStatus = hardwareStatus;
		return this;
	}

	public LocalDate getDeliveryDate() {
		return deliveryDate;
	}

	public void setDeliveryDate(LocalDate deliveryDate) {
		this.deliveryDate = deliveryDate;
	}

	public CreateAssetRequest withDeliveryDate(LocalDate deliveryDate) {
		this.deliveryDate = deliveryDate;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public CreateAssetRequest withMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		CreateAssetRequest that = (CreateAssetRequest) o;
		return Objects.equals(manufacturer, that.manufacturer) && Objects.equals(modelName, that.modelName) &&
			Objects.equals(modelDescription, that.modelDescription) && Objects.equals(serialNumber, that.serialNumber) &&
			Objects.equals(macAddress, that.macAddress) && Objects.equals(warrantyEndDate, that.warrantyEndDate) &&
			Objects.equals(supplierStatus, that.supplierStatus) && Objects.equals(hardwareStatus, that.hardwareStatus) &&
			Objects.equals(deliveryDate, that.deliveryDate) && Objects.equals(municipalityId, that.municipalityId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(manufacturer, modelName, modelDescription, serialNumber, macAddress, warrantyEndDate, supplierStatus, hardwareStatus, deliveryDate,
			municipalityId);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CreateAssetRequest [manufacturer=").append(manufacturer).append(", modelName=").append(modelName).append(", modelDescription=").append(modelDescription)
			.append(", serialNumber=").append(serialNumber).append(", macAddress=").append(macAddress).append(", warrantyEndDate=").append(warrantyEndDate).append(", supplierStatus=")
			.append(supplierStatus).append(", hardwareStatus=").append(hardwareStatus).append(", deliveryDate=").append(deliveryDate).append(", municipalityId=").append(municipalityId)
			.append("]");
		return builder.toString();
	}
}
