package se.sundsvall.supportcenter.service.mapper;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import generated.client.pob.PobMemo;
import generated.client.pob.PobMemo.StyleEnum;
import generated.client.pob.PobPayload;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportcenter.api.model.Asset;
import se.sundsvall.supportcenter.api.model.Note;
import se.sundsvall.supportcenter.api.model.UpdateAssetRequest;
import se.sundsvall.supportcenter.api.model.enums.NoteType;

class ConfigurationMapperTest {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	@Test
	void getClosureCodeList() {

		// Parameter values
		final var pobPayloadList = List.of(
			new PobPayload()
				.type("ClosureCode")
				.data(Map.of(
					"ClosureCodePK", "101",
					"Id", "ClosureCode-1")),
			new PobPayload()
				.type("ClosureCode")
				.data(Map.of(
					"ClosureCodePK", "102",
					"Id", "ClosureCode-2")),
			new PobPayload()
				.type("SomethingElseThanClosureCode")
				.data(Map.of(
					"ClosureCodePK", "103",
					"Id", "NotClosureCode-3")));

		// Call
		final var result = ConfigurationMapper.toClosureCodeList(pobPayloadList);

		// Verification
		assertThat(result).isNotNull().hasSize(2).containsExactly("ClosureCode-1", "ClosureCode-2");
	}

	@Test
	void getCaseCategoryList() {

		// Parameter values
		final var pobPayloadList = List.of(
			new PobPayload()
				.type("CaseCategory")
				.data(Map.of(
					"CaseCategoryPK", "101",
					"Id", "CaseCategory-1")),
			new PobPayload()
				.type("CaseCategory")
				.data(Map.of(
					"CaseCategoryPK", "102",
					"Id", "CaseCategory-2")),
			new PobPayload()
				.type("SomethingElseThanCaseCategory")
				.data(Map.of(
					"CaseCategoryPK", "103",
					"Id", "NotCaseCategory-3")));

		// Call
		final var result = ConfigurationMapper.toCaseCategoryList(pobPayloadList);

		// Verification
		assertThat(result).isNotNull().hasSize(2).containsExactly("CaseCategory-1", "CaseCategory-2");
	}

	@Test
	void getConfigurationItemList() {

		// Parameter values
		final var pobPayloadList = List.of(
			new PobPayload()
				.type("ConfigurationItem")
				.data(Map.of(
					"Id", "111")),
			new PobPayload()
				.type("ConfigurationItem")
				.data(Map.of(
					"Id", "222")),
			new PobPayload()
				.type("SomethingElseThanConfigurationItem")
				.data(Map.of(
					"Id", "000")));

		// Call
		final var result = ConfigurationMapper.toConfigurationItemList(pobPayloadList);

		// Verification
		assertThat(result).isNotNull().hasSize(2).containsExactly("111", "222");
	}

	@Test
	void getPobPayLoadWithNote() {
		final var note = Note.create().withType(NoteType.SUPPLIERNOTE).withText("text");
		final var deliveryDate = LocalDate.now().plusDays(1);
		final var warrantyEndDate = LocalDate.now().plusDays(700);

		// Parameter values
		final var updateAssetRequest = UpdateAssetRequest.create()
			.withHardwareName("hardwareName")
			.withMacAddress("macAddress")
			.withNote(note)
			.withSupplierStatus("supplierStatus")
			.withDeliveryDate(deliveryDate)
			.withWarrantyEndDate(warrantyEndDate);

		final var assetId = "assetId";

		// Call
		final var result = ConfigurationMapper.toPobPayload(assetId, updateAssetRequest);

		// Verification
		assertThat(result).isNotNull();
		assertThat(result.getData()).containsEntry("Id", assetId);
		assertThat(result.getData()).containsEntry("OptionalNumber", "hardwareName");
		assertThat(result.getData()).containsEntry("Virtual.BluetoothAddress", "macAddress");
		assertThat(result.getData()).containsEntry("Virtual.LeverantorensStatus", "supplierStatus");
		assertThat(result.getData()).containsEntry("DeliveryDate", deliveryDate.format(DATE_FORMATTER));
		assertThat(result.getData()).containsEntry("EndWarrantyDate", warrantyEndDate.format(DATE_FORMATTER));
		assertThat(result.getMemo()).extractingByKey("CILeverantorensAnteckningar").extracting(
			PobMemo::getExtension,
			PobMemo::getHandleSeparators,
			PobMemo::getIsValidForWeb,
			PobMemo::getMemo,
			PobMemo::getStyle)
			.containsExactly(
				".html",
				true,
				false,
				"text",
				StyleEnum.NUMBER_0);
	}

	@Test
	void getPobPayLoadWithoutNote() {
		// Parameter values
		final var deliveryDate = LocalDate.now().plusDays(1);
		final var warrantyEndDate = LocalDate.now().plusDays(700);

		final var updateAssetRequest = UpdateAssetRequest.create()
			.withHardwareName("hardwareName")
			.withMacAddress("macAddress")
			.withSupplierStatus("supplierStatus")
			.withDeliveryDate(deliveryDate)
			.withWarrantyEndDate(warrantyEndDate);

		final var assetId = "assetId";

		// Call
		final var result = ConfigurationMapper.toPobPayload(assetId, updateAssetRequest);

		// Verification
		assertThat(result).isNotNull();
		assertThat(result.getData()).containsEntry("Id", assetId);
		assertThat(result.getData()).containsEntry("OptionalNumber", "hardwareName");
		assertThat(result.getData()).containsEntry("Virtual.BluetoothAddress", "macAddress");
		assertThat(result.getData()).containsEntry("Virtual.LeverantorensStatus", "supplierStatus");
		assertThat(result.getData()).containsEntry("DeliveryDate", deliveryDate.format(DATE_FORMATTER));
		assertThat(result.getData()).containsEntry("EndWarrantyDate", warrantyEndDate.format(DATE_FORMATTER));
		assertThat(result.getMemo()).isEmpty();
	}

	@Test
	void toAssetList() {

		// Parameter values
		final var pobPayloadList = List.of(
			new PobPayload()
				.type("ConfigurationItem")
				.data(Map.of(
					"Id", "111",
					"Description", "hardwareDescription-1",
					"OptionalNumber", "hardwareName-1",
					"SerialNumber", "serialNumber-1",
					"CiStatus", "hardwareStatus-1",
					"Virtual.BluetoothAddress", "macAddress-1",
					"Virtual.LeverantorensStatus", "supplierStatus-1")),
			new PobPayload()
				.type("ConfigurationItem")
				.data(Map.of(
					"Id", "222",
					"Description", "hardwareDescription-2",
					"OptionalNumber", "hardwareName-2",
					"SerialNumber", "serialNumber-2",
					"CiStatus", "hardwareStatus-2",
					"Virtual.BluetoothAddress", "macAddress-2",
					"Virtual.LeverantorensStatus", "supplierStatus-2")),
			new PobPayload()
				.type("SomethingElseThanConfigurationItem")
				.data(Map.of(
					"Id", "333",
					"Description", "hardwareDescription-3",
					"OptionalNumber", "hardwareName-3",
					"SerialNumber", "serialNumber-3",
					"CiStatus", "hardwareStatus-3",
					"Virtual.BluetoothAddress", "macAddress-3",
					"Virtual.LeverantorensStatus", "supplierStatus-3")));

		// Call
		final var result = ConfigurationMapper.toAssetList(pobPayloadList);

		// Verification
		assertThat(result).hasSize(3);
		assertThat(result).extracting(Asset::getId).containsExactly("111", "222", "333");
		assertThat(result).extracting(Asset::getHardwareDescription).containsExactly("hardwareDescription-1", "hardwareDescription-2", "hardwareDescription-3");
		assertThat(result).extracting(Asset::getHardwareName).containsExactly("hardwareName-1", "hardwareName-2", "hardwareName-3");
		assertThat(result).extracting(Asset::getSerialNumber).containsExactly("serialNumber-1", "serialNumber-2", "serialNumber-3");
		assertThat(result).extracting(Asset::getHardwareStatus).containsExactly("hardwareStatus-1", "hardwareStatus-2", "hardwareStatus-3");
		assertThat(result).extracting(Asset::getMacAddress).containsExactly("macAddress-1", "macAddress-2", "macAddress-3");
		assertThat(result).extracting(Asset::getSupplierStatus).containsExactly("supplierStatus-1", "supplierStatus-2", "supplierStatus-3");
	}

	@Test
	void toAssetListNoResults() {

		// Parameter values
		final List<PobPayload> pobPayloadList = emptyList();

		// Call
		final var result = ConfigurationMapper.toAssetList(pobPayloadList);

		// Verification
		assertThat(result).isNotNull().isEmpty();
	}
}
