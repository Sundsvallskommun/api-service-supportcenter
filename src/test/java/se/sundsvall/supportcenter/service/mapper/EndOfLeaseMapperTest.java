package se.sundsvall.supportcenter.service.mapper;

import generated.client.pob.PobPayload;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchStatusCount;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus;

import static generated.client.sysman.SaveMessagesToTargetsCommand.TargetTypeEnum.COMPUTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.EXCLUDED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.FAILED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.SENT;
import static se.sundsvall.supportcenter.service.mapper.EndOfLeaseMapper.toAssetMunicipalityId;
import static se.sundsvall.supportcenter.service.mapper.EndOfLeaseMapper.toEndOfLeaseBatchEntity;
import static se.sundsvall.supportcenter.service.mapper.EndOfLeaseMapper.toEndOfLeaseBatchStatusResponse;
import static se.sundsvall.supportcenter.service.mapper.EndOfLeaseMapper.toEndOfLeaseStatisticsResponse;
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

	/**
	 * The rows arrive one per batch and state. What comes back is one entry per batch, in the order the rows came in,
	 * with the totals added up from the same rows so that the summary cannot disagree with the batches under it.
	 */
	@Test
	void toEndOfLeaseStatisticsResponseGroupsTheCountsPerBatch() {
		final var newerCreated = OffsetDateTime.parse("2026-09-17T06:03:11+02:00");
		final var olderCreated = OffsetDateTime.parse("2026-09-16T06:03:11+02:00");

		final var counts = List.of(
			new EndOfLeaseBatchStatusCount("newer", "ADV-2", newerCreated, PENDING, 4),
			new EndOfLeaseBatchStatusCount("newer", "ADV-2", newerCreated, EXCLUDED, 1),
			new EndOfLeaseBatchStatusCount("older", "ADV-1", olderCreated, SENT, 20),
			new EndOfLeaseBatchStatusCount("older", "ADV-1", olderCreated, FAILED, 2));

		final var statistics = toEndOfLeaseStatisticsResponse(counts, LocalDate.of(2026, 8, 18), LocalDate.of(2026, 9, 18));

		assertThat(statistics.getFrom()).isEqualTo(LocalDate.of(2026, 8, 18));
		assertThat(statistics.getTo()).isEqualTo(LocalDate.of(2026, 9, 18));
		assertThat(statistics.getBatches()).isEqualTo(2);
		assertThat(statistics.getComputers().getTotal()).isEqualTo(27);
		assertThat(statistics.getComputers().getPending()).isEqualTo(4);
		assertThat(statistics.getComputers().getSent()).isEqualTo(20);
		assertThat(statistics.getComputers().getFailed()).isEqualTo(2);
		assertThat(statistics.getComputers().getExcluded()).isEqualTo(1);

		assertThat(statistics.getPerBatch())
			.extracting("id", "externalBatchId", "created", "total", "pending", "sent", "failed", "excluded")
			.containsExactly(
				tuple("newer", "ADV-2", newerCreated, 5L, 4L, 0L, 0L, 1L),
				tuple("older", "ADV-1", olderCreated, 22L, 0L, 20L, 2L, 0L));
	}

	/**
	 * A window with nothing in it is still the window that was asked for, which is what tells a quiet month apart from a
	 * service nobody has ever sent anything to.
	 */
	@Test
	void toEndOfLeaseStatisticsResponseAnswersZeroForAMunicipalityThatHasRegisteredNothing() {
		final var statistics = toEndOfLeaseStatisticsResponse(List.of(), LocalDate.of(2026, 8, 18), LocalDate.of(2026, 9, 18));

		assertThat(statistics.getFrom()).isEqualTo(LocalDate.of(2026, 8, 18));
		assertThat(statistics.getTo()).isEqualTo(LocalDate.of(2026, 9, 18));
		assertThat(statistics.getBatches()).isZero();
		assertThat(statistics.getPerBatch()).isEmpty();
		assertThat(statistics.getComputers()).isNotNull();
		assertThat(statistics.getComputers().getTotal()).isZero();
		assertThat(statistics.getComputers().getPending()).isZero();
		assertThat(statistics.getComputers().getSent()).isZero();
		assertThat(statistics.getComputers().getFailed()).isZero();
		assertThat(statistics.getComputers().getExcluded()).isZero();
	}

	@Test
	void toEndOfLeaseBatchStatusResponseCountsTheComputersItLists() {
		final var created = OffsetDateTime.parse("2026-09-17T06:03:11+02:00");
		final var sentAt = OffsetDateTime.parse("2026-09-17T06:14:52+02:00");

		final var batch = EndOfLeaseBatchEntity.create()
			.withId("8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c")
			.withExternalBatchId(EXTERNAL_BATCH_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withCreated(created);

		final var computers = List.of(
			computerEntity("J111AAA", "AB11111", PENDING).withAttempts(1),
			computerEntity("J222BBB", "AB22222", SENT).withAttempts(3).withSentAt(sentAt),
			computerEntity("J333CCC", "AB33333", FAILED).withAttempts(5).withErrorMessage("POB is unwell"),
			computerEntity("J444DDD", "PUB44444", EXCLUDED).withAttempts(0));

		final var response = toEndOfLeaseBatchStatusResponse(batch, computers, null);

		assertThat(response.getId()).isEqualTo("8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c");
		assertThat(response.getExternalBatchId()).isEqualTo(EXTERNAL_BATCH_ID);
		assertThat(response.getCreated()).isEqualTo(created);
		assertThat(response.getTotal()).isEqualTo(4);
		assertThat(response.getPending()).isEqualTo(1);
		assertThat(response.getSent()).isEqualTo(1);
		assertThat(response.getFailed()).isEqualTo(1);
		assertThat(response.getExcluded()).isEqualTo(1);

		assertThat(response.getComputers())
			.extracting("serialNumber", "assetTag", "status", "attempts", "errorMessage", "sentAt")
			.containsExactly(
				tuple("J111AAA", "AB11111", "PENDING", 1, null, null),
				tuple("J222BBB", "AB22222", "SENT", 3, null, sentAt),
				tuple("J333CCC", "AB33333", "FAILED", 5, "POB is unwell", null),
				tuple("J444DDD", "PUB44444", "EXCLUDED", 0, null, null));
	}

	@Test
	void toEndOfLeaseBatchStatusResponseAnswersWithAnEmptyListForABatchWithNoComputers() {
		final var batch = EndOfLeaseBatchEntity.create()
			.withId("8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c")
			.withExternalBatchId(EXTERNAL_BATCH_ID);

		final var response = toEndOfLeaseBatchStatusResponse(batch, List.of(), null);

		assertThat(response.getTotal()).isZero();
		assertThat(response.getComputers()).isEmpty();
	}

	/**
	 * The attempts are set the way the column is: not null, and zero until something spends one. A row that reaches the
	 * mapper has been through the database and always carries a number.
	 */
	private static EndOfLeaseComputerEntity computerEntity(final String serialNumber, final String assetTag, final EndOfLeaseStatus status) {
		return EndOfLeaseComputerEntity.create()
			.withSerialNumber(serialNumber)
			.withAssetTag(assetTag)
			.withEndOfLeaseDate(END_OF_LEASE_DATE)
			.withStatus(status)
			.withAttempts(0);
	}

	/**
	 * The states asked for decide what is listed, and nothing else. The counts stay over the whole batch, which is what
	 * lets three failures be read as three out of four rather than as the whole of it.
	 */
	@Test
	void toEndOfLeaseBatchStatusResponseListsOnlyTheStatesAskedFor() {
		final var batch = EndOfLeaseBatchEntity.create().withId("batchId").withExternalBatchId(EXTERNAL_BATCH_ID);

		final var computers = List.of(
			computerEntity("J111AAA", "AB11111", PENDING),
			computerEntity("J222BBB", "AB22222", SENT),
			computerEntity("J333CCC", "AB33333", FAILED).withAttempts(5).withErrorMessage("POB is unwell"),
			computerEntity("J444DDD", "PUB44444", EXCLUDED));

		final var response = toEndOfLeaseBatchStatusResponse(batch, computers, List.of("FAILED"));

		assertThat(response.getComputers())
			.extracting("serialNumber", "status")
			.containsExactly(tuple("J333CCC", "FAILED"));
		assertThat(response.getTotal()).isEqualTo(4);
		assertThat(response.getSent()).isEqualTo(1);
		assertThat(response.getPending()).isEqualTo(1);
		assertThat(response.getExcluded()).isEqualTo(1);
	}

	@Test
	void toEndOfLeaseBatchStatusResponseListsSeveralStatesAtOnce() {
		final var batch = EndOfLeaseBatchEntity.create().withId("batchId").withExternalBatchId(EXTERNAL_BATCH_ID);

		final var computers = List.of(
			computerEntity("J111AAA", "AB11111", PENDING),
			computerEntity("J222BBB", "AB22222", SENT),
			computerEntity("J333CCC", "AB33333", FAILED));

		final var response = toEndOfLeaseBatchStatusResponse(batch, computers, List.of("PENDING", "FAILED"));

		assertThat(response.getComputers())
			.extracting("serialNumber")
			.containsExactly("J111AAA", "J333CCC");
	}

	/**
	 * A state nothing in the batch is in is not an error, it is a batch with nothing in that state.
	 */
	@Test
	void toEndOfLeaseBatchStatusResponseForAStateNoComputerIsIn() {
		final var batch = EndOfLeaseBatchEntity.create().withId("batchId").withExternalBatchId(EXTERNAL_BATCH_ID);

		final var response = toEndOfLeaseBatchStatusResponse(batch, List.of(computerEntity("J111AAA", "AB11111", SENT)), List.of("FAILED"));

		assertThat(response.getComputers()).isEmpty();
		assertThat(response.getTotal()).isEqualTo(1);
		assertThat(response.getSent()).isEqualTo(1);
	}

	@Test
	void toEndOfLeaseBatchStatusResponseListsEveryStateForAnEmptyFilter() {
		final var batch = EndOfLeaseBatchEntity.create().withId("batchId").withExternalBatchId(EXTERNAL_BATCH_ID);

		final var computers = List.of(
			computerEntity("J111AAA", "AB11111", PENDING),
			computerEntity("J222BBB", "AB22222", SENT));

		assertThat(toEndOfLeaseBatchStatusResponse(batch, computers, List.of()).getComputers()).hasSize(2);
	}
}
