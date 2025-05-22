package se.sundsvall.supportcenter.api.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.LocalDate;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssetTest {

	@BeforeEach
	void setup() {
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt()), LocalDate.class);
	}

	@Test
	void isValidBean() {
		assertThat(Asset.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var id = "id";
		final var serialNumber = "serialNumber";
		final var hardwareName = "hardwareName";
		final var hardwareDescription = "hardwareDescription";
		final var macAddress = "macAddress";
		final var supplierStatus = "supplierStatus";
		final var hardwareStatus = "hardwareStatus";
		final var modelName = "modelName";
		final var modelDescription = "modelDescription";
		final var manufacturer = "manufacturer";
		final var deliveryDate = LocalDate.now().plusDays(3);
		final var warrantyEndDate = LocalDate.now().plusDays(365);
		final var municipalityId = "municipalityId";
		final var leaseStatus = "leaseStatus";

		final var asset = Asset.create()
			.withId(id)
			.withSerialNumber(serialNumber)
			.withHardwareName(hardwareName)
			.withHardwareDescription(hardwareDescription)
			.withMacAddress(macAddress)
			.withSupplierStatus(supplierStatus)
			.withHardwareStatus(hardwareStatus)
			.withManufacturer(manufacturer)
			.withModelName(modelName)
			.withModelDescription(modelDescription)
			.withDeliveryDate(deliveryDate)
			.withWarrantyEndDate(warrantyEndDate)
			.withMunicipalityId(municipalityId)
			.withLeaseStatus(leaseStatus);

		assertThat(asset).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(asset.getId()).isEqualTo(id);
		assertThat(asset.getSerialNumber()).isEqualTo(serialNumber);
		assertThat(asset.getHardwareName()).isEqualTo(hardwareName);
		assertThat(asset.getHardwareDescription()).isEqualTo(hardwareDescription);
		assertThat(asset.getMacAddress()).isEqualTo(macAddress);
		assertThat(asset.getSupplierStatus()).isEqualTo(supplierStatus);
		assertThat(asset.getHardwareStatus()).isEqualTo(hardwareStatus);
		assertThat(asset.getManufacturer()).isEqualTo(manufacturer);
		assertThat(asset.getModelName()).isEqualTo(modelName);
		assertThat(asset.getModelDescription()).isEqualTo(modelDescription);
		assertThat(asset.getDeliveryDate()).isEqualTo(deliveryDate);
		assertThat(asset.getWarrantyEndDate()).isEqualTo(warrantyEndDate);
		assertThat(asset.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(asset.getLeaseStatus()).isEqualTo(leaseStatus);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(Asset.create()).hasAllNullFieldsOrProperties();
	}
}
