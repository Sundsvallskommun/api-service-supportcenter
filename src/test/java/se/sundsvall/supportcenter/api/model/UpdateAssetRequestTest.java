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

class UpdateAssetRequestTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), LocalDate.class);
	}

	@Test
	void isValidBean() {
		assertThat(UpdateAssetRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var note = Note.create();
		final var hardwareName = "hardwareName";
		final var macAddress = "macAddress";
		final var supplierStatus = "supplierStatus";
		final var hardwareStatus = "hardwareStatus";
		final var deliveryDate = LocalDate.now().plusDays(1);
		final var warrantyEndDate = LocalDate.now().plusDays(700);
		final var municipalityId = "municipalityId";
		final var leaseStatus = "leaseStatus";

		final var updateCaseRequest = UpdateAssetRequest.create()
			.withNote(note)
			.withHardwareName(hardwareName)
			.withMacAddress(macAddress)
			.withSupplierStatus(supplierStatus)
			.withHardwareStatus(hardwareStatus)
			.withDeliveryDate(deliveryDate)
			.withWarrantyEndDate(warrantyEndDate)
			.withMunicipalityId(municipalityId)
			.withLeaseStatus(leaseStatus);

		assertThat(updateCaseRequest).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(updateCaseRequest.getNote()).isEqualTo(note);
		assertThat(updateCaseRequest.getHardwareName()).isEqualTo(hardwareName);
		assertThat(updateCaseRequest.getMacAddress()).isEqualTo(macAddress);
		assertThat(updateCaseRequest.getSupplierStatus()).isEqualTo(supplierStatus);
		assertThat(updateCaseRequest.getHardwareStatus()).isEqualTo(hardwareStatus);
		assertThat(updateCaseRequest.getDeliveryDate()).isEqualTo(deliveryDate);
		assertThat(updateCaseRequest.getWarrantyEndDate()).isEqualTo(warrantyEndDate);
		assertThat(updateCaseRequest.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(updateCaseRequest.getLeaseStatus()).isEqualTo(leaseStatus);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(UpdateCaseRequest.create()).hasAllNullFieldsOrProperties();
	}
}
