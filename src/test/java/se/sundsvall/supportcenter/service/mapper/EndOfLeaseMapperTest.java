package se.sundsvall.supportcenter.service.mapper;

import generated.client.pob.PobPayload;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputer;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus;

import static generated.client.sysman.SaveMessagesToTargetsCommand.TargetTypeEnum.COMPUTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;
import static se.sundsvall.supportcenter.service.mapper.EndOfLeaseMapper.toAssetMunicipalityId;
import static se.sundsvall.supportcenter.service.mapper.EndOfLeaseMapper.toEndOfLeaseBatchEntity;
import static se.sundsvall.supportcenter.service.mapper.EndOfLeaseMapper.toErrorMessage;
import static se.sundsvall.supportcenter.service.mapper.EndOfLeaseMapper.toSaveMessagesToTargetsCommand;

class EndOfLeaseMapperTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String EXTERNAL_BATCH_ID = "d1f3a8c2-9b7e-4a5f-8c3d-2e6b1a4f7c90";
	private static final LocalDate END_OF_LEASE_DATE = LocalDate.of(2026, 11, 30);

	@Test
	void toEndOfLeaseBatchEntityMapsAllComputers() {
		final var createEndOfLeaseBatchRequest = CreateEndOfLeaseBatchRequest.create()
			.withExternalBatchId(EXTERNAL_BATCH_ID)
			.withComputers(List.of(
				EndOfLeaseComputer.create()
					.withSerialNumber("J123ABC")
					.withAssetTag("AB12345")
					.withEndOfLeaseDate(END_OF_LEASE_DATE),
				EndOfLeaseComputer.create()
					.withSerialNumber("K456DEF")
					.withAssetTag("PUB16604")
					.withEndOfLeaseDate(END_OF_LEASE_DATE)));

		final var endOfLeaseBatchEntity = toEndOfLeaseBatchEntity(MUNICIPALITY_ID, createEndOfLeaseBatchRequest);

		assertThat(endOfLeaseBatchEntity.getExternalBatchId()).isEqualTo(EXTERNAL_BATCH_ID);
		assertThat(endOfLeaseBatchEntity.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(endOfLeaseBatchEntity.getComputers())
			.extracting(EndOfLeaseComputerEntity::getSerialNumber, EndOfLeaseComputerEntity::getAssetTag, EndOfLeaseComputerEntity::getEndOfLeaseDate)
			.containsExactly(
				tuple("J123ABC", "AB12345", END_OF_LEASE_DATE),
				tuple("K456DEF", "PUB16604", END_OF_LEASE_DATE));
	}

	@Test
	void toEndOfLeaseBatchEntitySetsEveryComputerToPendingAndPointsItBackAtTheBatch() {
		final var createEndOfLeaseBatchRequest = CreateEndOfLeaseBatchRequest.create()
			.withExternalBatchId(EXTERNAL_BATCH_ID)
			.withComputers(List.of(EndOfLeaseComputer.create()
				.withSerialNumber("J123ABC")
				.withAssetTag("AB12345")
				.withEndOfLeaseDate(END_OF_LEASE_DATE)));

		final var endOfLeaseBatchEntity = toEndOfLeaseBatchEntity(MUNICIPALITY_ID, createEndOfLeaseBatchRequest);

		assertThat(endOfLeaseBatchEntity.getComputers()).hasSize(1).allSatisfy(computer -> {
			assertThat(computer.getSerialNumber()).isEqualTo("J123ABC");
			assertThat(computer.getAssetTag()).isEqualTo("AB12345");
			assertThat(computer.getEndOfLeaseDate()).isEqualTo(END_OF_LEASE_DATE);
			assertThat(computer.getStatus()).isEqualTo(PENDING);
			assertThat(computer.getBatch()).isSameAs(endOfLeaseBatchEntity);
			assertThat(computer.getAssetMunicipalityId()).isNull();
			assertThat(computer.getSentAt()).isNull();
		});
	}

	@ParameterizedTest
	@CsvSource({
		"EB12345, EXCLUDED",
		"LB12345, EXCLUDED",
		"PB12345, EXCLUDED",
		"PS12345, EXCLUDED",
		"MPB12345, EXCLUDED",
		"MPS12345, EXCLUDED",
		"SP12345, EXCLUDED",
		"CB12345, EXCLUDED",
		"PUB16604, EXCLUDED",
		"pub16604, EXCLUDED",
		"AB12345, PENDING",
		"WB16603, PENDING",
		"SPARE1, PENDING",
		"12345, PENDING"
	})
	void toEndOfLeaseBatchEntityHoldsBackComputerTypesThatAreNotSent(final String assetTag, final EndOfLeaseStatus expectedStatus) {
		final var createEndOfLeaseBatchRequest = CreateEndOfLeaseBatchRequest.create()
			.withExternalBatchId(EXTERNAL_BATCH_ID)
			.withComputers(List.of(EndOfLeaseComputer.create()
				.withSerialNumber("J123ABC")
				.withAssetTag(assetTag)
				.withEndOfLeaseDate(END_OF_LEASE_DATE)));

		final var endOfLeaseBatchEntity = toEndOfLeaseBatchEntity(MUNICIPALITY_ID, createEndOfLeaseBatchRequest);

		assertThat(endOfLeaseBatchEntity.getComputers())
			.extracting(EndOfLeaseComputerEntity::getStatus)
			.containsExactly(expectedStatus);
	}

	/**
	 * POB holds the municipality as either the id itself or the name it belongs to, and a computer has to be routable
	 * whichever one it was written with.
	 */
	@ParameterizedTest
	@CsvSource({
		"2281, 2281",
		"Sundsvall, 2281",
		"sundsvall, 2281",
		"2260, 2260",
		"Ånge, 2260"
	})
	void toAssetMunicipalityIdReadsBothSpellings(final String municipality, final String expectedMunicipalityId) {
		assertThat(toAssetMunicipalityId(configurationItem(municipality))).isEqualTo(expectedMunicipalityId);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@CsvSource({
		"Timrå", "9999"
	})
	void toAssetMunicipalityIdAnswersNullForAMunicipalityWeCannotRouteOn(final String municipality) {
		assertThat(toAssetMunicipalityId(configurationItem(municipality))).isNull();
	}

	/**
	 * The same computer can legitimately sit in more than one batch, and a page can hold both rows. Naming it twice in
	 * one call asks the installation to do the same work twice.
	 */
	@Test
	void toSaveMessagesToTargetsCommandNamesEachComputerOnce() {
		final var command = toSaveMessagesToTargetsCommand(List.of(
			EndOfLeaseComputerEntity.create().withAssetTag("AB12345").withSerialNumber("J123ABC"),
			EndOfLeaseComputerEntity.create().withAssetTag("AB12345").withSerialNumber("J123ABC"),
			EndOfLeaseComputerEntity.create().withAssetTag("CD67890").withSerialNumber("K456DEF")), 42L);

		assertThat(command.getTargets()).containsExactly("AB12345", "CD67890");
	}

	/**
	 * POB holds Data as name to object, so nothing in the contract says the municipality arrives as a string. Read with
	 * a cast it would be a ClassCastException, and the run would spend the computer's attempt on a value that routes
	 * perfectly well written out.
	 */
	@Test
	void toAssetMunicipalityIdReadsAMunicipalityThatCameBackAsANumber() {
		final var configurationItem = new PobPayload().type("ConfigurationItem").data(new HashMap<>(Map.of("Virtual.CIKommun", 2281)));

		assertThat(toAssetMunicipalityId(List.of(configurationItem))).isEqualTo("2281");
	}

	@ParameterizedTest
	@NullSource
	void toAssetMunicipalityIdAnswersNullWhenPobKnowsNoSuchComputer(final List<PobPayload> configurationItems) {
		assertThat(toAssetMunicipalityId(configurationItems)).isNull();
	}

	@Test
	void toAssetMunicipalityIdAnswersNullForAnEmptyAnswer() {
		assertThat(toAssetMunicipalityId(List.of())).isNull();
	}

	@Test
	void toAssetMunicipalityIdAnswersNullForAConfigurationItemWithoutData() {
		assertThat(toAssetMunicipalityId(List.of(new PobPayload().type("ConfigurationItem")))).isNull();
	}

	private static List<PobPayload> configurationItem(final String municipality) {
		final var dataMap = new HashMap<String, Object>();
		dataMap.put("Virtual.CIKommun", municipality);

		return List.of(new PobPayload().type("ConfigurationItem").data(dataMap));
	}

	/**
	 * A computer is known to SysMan by its asset tag, which is its computer name there. targetAll has to stay false,
	 * since true would take the message past the named targets to every computer in the installation.
	 */
	@Test
	void toSaveMessagesToTargetsCommandNamesTheComputersByAssetTag() {
		final var command = toSaveMessagesToTargetsCommand(List.of(
			EndOfLeaseComputerEntity.create().withAssetTag("AB12345").withSerialNumber("J123ABC"),
			EndOfLeaseComputerEntity.create().withAssetTag("CD67890").withSerialNumber("K456DEF")), 7);

		assertThat(command.getTargets()).containsExactly("AB12345", "CD67890");
		assertThat(command.getMessagesToSend()).containsExactly(7L);
		assertThat(command.getTargetType()).isEqualTo(COMPUTER);
		assertThat(command.getTargetAll()).isFalse();
	}

	/**
	 * A server that answers with its own stack trace produces a reason several times wider than the column. Stored as
	 * it comes, the insert is rejected from inside the catch block that was recording the failure, which takes the run
	 * down and leaves the row to do it again on the next one.
	 */
	@Test
	void toErrorMessageCutsAReasonToWhatTheColumnHolds() {
		final var tooLong = "x".repeat(3000);

		assertThat(toErrorMessage(tooLong)).hasSize(2048);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@CsvSource({
		"POB is unwell"
	})
	void toErrorMessageLeavesAReasonThatFitsAlone(final String errorMessage) {
		assertThat(toErrorMessage(errorMessage)).isEqualTo(errorMessage);
	}
}
