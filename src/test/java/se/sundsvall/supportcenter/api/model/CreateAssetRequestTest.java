package se.sundsvall.supportcenter.api.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CreateAssetRequestTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), LocalDate.class);
	}

	@Test
	void isValidBean() {
		assertThat(CreateAssetRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var manufacturer = "manufacturer";
		final var modelName = "modelName";
		final var modelDescription = "modelDescription";
		final var serialNumber = "serialNumber";
		final var macAddress = "macAddress";
		final var warrantyEndDate = LocalDate.now().plusDays(360L);
		final var supplierStatus = "supplierStatus";
		final var hardwareStatus = "hardwareStatus";
		final var deliveryDate = LocalDate.now().plusDays(5L);
		final var municipalityId = "municipalityId";
		final var leaseStatus = "leaseStatus";

		final var createAssetRequest = CreateAssetRequest.create()
			.withManufacturer(manufacturer)
			.withModelName(modelName)
			.withModelDescription(modelDescription)
			.withSerialNumber(serialNumber)
			.withMacAddress(macAddress)
			.withWarrantyEndDate(warrantyEndDate)
			.withSupplierStatus(supplierStatus)
			.withHardwareStatus(hardwareStatus)
			.withDeliveryDate(deliveryDate)
			.withMunicipalityId(municipalityId)
			.withLeaseStatus(leaseStatus);

		assertThat(createAssetRequest).hasNoNullFieldsOrProperties();
		assertThat(createAssetRequest.getManufacturer()).isEqualTo(manufacturer);
		assertThat(createAssetRequest.getModelName()).isEqualTo(modelName);
		assertThat(createAssetRequest.getModelDescription()).isEqualTo(modelDescription);
		assertThat(createAssetRequest.getSerialNumber()).isEqualTo(serialNumber);
		assertThat(createAssetRequest.getMacAddress()).isEqualTo(macAddress);
		assertThat(createAssetRequest.getWarrantyEndDate()).isEqualTo(warrantyEndDate);
		assertThat(createAssetRequest.getSupplierStatus()).isEqualTo(supplierStatus);
		assertThat(createAssetRequest.getHardwareStatus()).isEqualTo(hardwareStatus);
		assertThat(createAssetRequest.getDeliveryDate()).isEqualTo(deliveryDate);
		assertThat(createAssetRequest.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(createAssetRequest.getLeaseStatus()).isEqualTo(leaseStatus);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(CreateAssetRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new CreateAssetRequest()).hasAllNullFieldsOrProperties();
	}
}
