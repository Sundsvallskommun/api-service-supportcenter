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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.EndOfLeaseBatchStatistics;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputer;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchStatusCount;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatusCount;

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
	 * The rows arrive one per batch and state. The batches of the page decide what comes back and in which order, and
	 * the counts are looked up against them. The window counts are their own query and are not the page added up, which
	 * is what lets page two still say what the whole window holds.
	 */
	@Test
	void toEndOfLeaseStatisticsResponseCountsEachBatchOfThePage() {
		final var newerCreated = OffsetDateTime.parse("2026-09-17T06:03:11+02:00");
		final var olderCreated = OffsetDateTime.parse("2026-09-16T06:03:11+02:00");

		final var batches = new PageImpl<>(List.of(
			batchEntity("newer", "ADV-2", newerCreated),
			batchEntity("older", "ADV-1", olderCreated)), PageRequest.of(0, 100), 2);

		final var countsOfThePage = List.of(
			new EndOfLeaseBatchStatusCount("newer", "ADV-2", newerCreated, PENDING, 4),
			new EndOfLeaseBatchStatusCount("newer", "ADV-2", newerCreated, EXCLUDED, 1),
			new EndOfLeaseBatchStatusCount("older", "ADV-1", olderCreated, SENT, 20),
			new EndOfLeaseBatchStatusCount("older", "ADV-1", olderCreated, FAILED, 2));

		final var countsOfTheWindow = List.of(
			new EndOfLeaseStatusCount(PENDING, 4),
			new EndOfLeaseStatusCount(SENT, 20),
			new EndOfLeaseStatusCount(FAILED, 2),
			new EndOfLeaseStatusCount(EXCLUDED, 1));

		final var statistics = toEndOfLeaseStatisticsResponse(batches, countsOfThePage, countsOfTheWindow, LocalDate.of(2026, 8, 18), LocalDate.of(2026, 9, 18));

		assertThat(statistics.getFrom()).isEqualTo(LocalDate.of(2026, 8, 18));
		assertThat(statistics.getTo()).isEqualTo(LocalDate.of(2026, 9, 18));
		assertThat(statistics.getCounts().getTotal()).isEqualTo(27);
		assertThat(statistics.getCounts().getPending()).isEqualTo(4);
		assertThat(statistics.getCounts().getSent()).isEqualTo(20);
		assertThat(statistics.getCounts().getFailed()).isEqualTo(2);
		assertThat(statistics.getCounts().getExcluded()).isEqualTo(1);

		assertThat(statistics.getBatches())
			.extracting("id", "externalBatchId", "created")
			.containsExactly(
				tuple("newer", "ADV-2", newerCreated),
				tuple("older", "ADV-1", olderCreated));

		assertThat(statistics.getBatches())
			.extracting(EndOfLeaseBatchStatistics::getCounts)
			.extracting("total", "pending", "sent", "failed", "excluded")
			.containsExactly(
				tuple(5L, 4L, 0L, 0L, 1L),
				tuple(22L, 0L, 20L, 2L, 0L));

		assertThat(statistics.getMetadata().getTotalRecords()).isEqualTo(2);
	}

	/**
	 * The window counts cover every batch between the two days, and the page is one slice of them. A reader on the
	 * second page is still told what the whole window holds, which is the difference between three failures out of the
	 * page and three out of the month.
	 */
	@Test
	void toEndOfLeaseStatisticsResponseCountsTheWindowAndNotThePage() {
		final var created = OffsetDateTime.parse("2026-09-17T06:03:11+02:00");

		final var batches = new PageImpl<>(List.of(batchEntity("onThePage", "ADV-9", created)), PageRequest.of(1, 1), 40);

		final var statistics = toEndOfLeaseStatisticsResponse(
			batches,
			List.of(new EndOfLeaseBatchStatusCount("onThePage", "ADV-9", created, SENT, 7)),
			List.of(new EndOfLeaseStatusCount(SENT, 900), new EndOfLeaseStatusCount(FAILED, 12)),
			LocalDate.of(2026, 8, 18),
			LocalDate.of(2026, 9, 18));

		assertThat(statistics.getCounts().getTotal()).isEqualTo(912);
		assertThat(statistics.getCounts().getFailed()).isEqualTo(12);
		assertThat(statistics.getBatches()).hasSize(1);
		assertThat(statistics.getBatches().getFirst().getCounts().getTotal()).isEqualTo(7);

		assertThat(statistics.getMetadata().getPage()).isEqualTo(2);
		assertThat(statistics.getMetadata().getLimit()).isEqualTo(1);
		assertThat(statistics.getMetadata().getCount()).isEqualTo(1);
		assertThat(statistics.getMetadata().getTotalRecords()).isEqualTo(40);
		assertThat(statistics.getMetadata().getTotalPages()).isEqualTo(40);
	}

	/**
	 * A batch is registered with at least one computer, so a batch the counts know nothing about should not happen.
	 * Answering it with zeroes rather than dropping it keeps the page the size the metadata says it is.
	 */
	@Test
	void toEndOfLeaseStatisticsResponseAnswersZeroForABatchTheCountsMissed() {
		final var created = OffsetDateTime.parse("2026-09-17T06:03:11+02:00");

		final var statistics = toEndOfLeaseStatisticsResponse(
			new PageImpl<>(List.of(batchEntity("counted", "ADV-2", created), batchEntity("uncounted", "ADV-3", created)), PageRequest.of(0, 100), 2),
			List.of(new EndOfLeaseBatchStatusCount("counted", "ADV-2", created, SENT, 3)),
			List.of(new EndOfLeaseStatusCount(SENT, 3)),
			LocalDate.of(2026, 8, 18),
			LocalDate.of(2026, 9, 18));

		assertThat(statistics.getBatches()).hasSize(2);
		assertThat(statistics.getBatches().getFirst().getCounts().getSent()).isEqualTo(3);
		assertThat(statistics.getBatches().getLast().getCounts().getTotal()).isZero();
		assertThat(statistics.getBatches().getLast().getCounts().getSent()).isZero();
	}

	/**
	 * A window with nothing in it is still the window that was asked for, which is what tells a quiet month apart from a
	 * service nobody has ever sent anything to.
	 */
	@Test
	void toEndOfLeaseStatisticsResponseAnswersZeroForAMunicipalityThatHasRegisteredNothing() {
		final var statistics = toEndOfLeaseStatisticsResponse(
			new PageImpl<>(List.<EndOfLeaseBatchEntity>of(), PageRequest.of(0, 100), 0),
			List.of(),
			List.of(),
			LocalDate.of(2026, 8, 18),
			LocalDate.of(2026, 9, 18));

		assertThat(statistics.getFrom()).isEqualTo(LocalDate.of(2026, 8, 18));
		assertThat(statistics.getTo()).isEqualTo(LocalDate.of(2026, 9, 18));
		assertThat(statistics.getBatches()).isEmpty();
		assertThat(statistics.getCounts()).isNotNull();
		assertThat(statistics.getCounts().getTotal()).isZero();
		assertThat(statistics.getCounts().getPending()).isZero();
		assertThat(statistics.getCounts().getSent()).isZero();
		assertThat(statistics.getCounts().getFailed()).isZero();
		assertThat(statistics.getCounts().getExcluded()).isZero();
		assertThat(statistics.getMetadata().getTotalRecords()).isZero();
	}

	/**
	 * The counts come from the query over the whole batch, and the list is the page. Four hundred computers answered ten
	 * at a time still say four hundred, which is the only reason the two are separate arguments.
	 */
	@Test
	void toEndOfLeaseBatchStatusResponseCountsTheWholeBatchAndListsThePage() {
		final var created = OffsetDateTime.parse("2026-09-17T06:03:11+02:00");
		final var sentAt = OffsetDateTime.parse("2026-09-17T06:14:52+02:00");

		final var batch = batchEntity("8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c", EXTERNAL_BATCH_ID, created);

		final var computers = new PageImpl<>(List.of(
			computerEntity("J111AAA", "AB11111", PENDING).withAttempts(1),
			computerEntity("J222BBB", "AB22222", SENT).withAttempts(3).withSentAt(sentAt),
			computerEntity("J333CCC", "AB33333", FAILED).withAttempts(5).withErrorMessage("POB is unwell"),
			computerEntity("J444DDD", "PUB44444", EXCLUDED).withAttempts(0)), PageRequest.of(0, 4), 4);

		final var countsOfTheBatch = List.of(
			new EndOfLeaseBatchStatusCount("8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c", EXTERNAL_BATCH_ID, created, PENDING, 1),
			new EndOfLeaseBatchStatusCount("8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c", EXTERNAL_BATCH_ID, created, SENT, 1),
			new EndOfLeaseBatchStatusCount("8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c", EXTERNAL_BATCH_ID, created, FAILED, 1),
			new EndOfLeaseBatchStatusCount("8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c", EXTERNAL_BATCH_ID, created, EXCLUDED, 1));

		final var response = toEndOfLeaseBatchStatusResponse(batch, computers, countsOfTheBatch);

		assertThat(response.getId()).isEqualTo("8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c");
		assertThat(response.getExternalBatchId()).isEqualTo(EXTERNAL_BATCH_ID);
		assertThat(response.getCreated()).isEqualTo(created);
		assertThat(response.getCounts().getTotal()).isEqualTo(4);
		assertThat(response.getCounts().getPending()).isEqualTo(1);
		assertThat(response.getCounts().getSent()).isEqualTo(1);
		assertThat(response.getCounts().getFailed()).isEqualTo(1);
		assertThat(response.getCounts().getExcluded()).isEqualTo(1);

		assertThat(response.getComputers())
			.extracting("serialNumber", "assetTag", "status", "attempts", "errorMessage", "sentAt")
			.containsExactly(
				tuple("J111AAA", "AB11111", "PENDING", 1, null, null),
				tuple("J222BBB", "AB22222", "SENT", 3, null, sentAt),
				tuple("J333CCC", "AB33333", "FAILED", 5, "POB is unwell", null),
				tuple("J444DDD", "PUB44444", "EXCLUDED", 0, null, null));
	}

	/**
	 * The page holds what the states asked for left of it, the counts still hold the batch. Three failures read as three
	 * out of nine hundred rather than as the whole of it, which is the point of counting separately.
	 */
	@Test
	void toEndOfLeaseBatchStatusResponseCountsTheBatchTheFilteredPageCameFrom() {
		final var created = OffsetDateTime.parse("2026-09-17T06:03:11+02:00");
		final var batch = batchEntity("batchId", EXTERNAL_BATCH_ID, created);

		final var response = toEndOfLeaseBatchStatusResponse(
			batch,
			new PageImpl<>(List.of(computerEntity("J333CCC", "AB33333", FAILED).withAttempts(5).withErrorMessage("POB is unwell")), PageRequest.of(0, 1), 3),
			List.of(
				new EndOfLeaseBatchStatusCount("batchId", EXTERNAL_BATCH_ID, created, SENT, 897),
				new EndOfLeaseBatchStatusCount("batchId", EXTERNAL_BATCH_ID, created, FAILED, 3)));

		assertThat(response.getComputers())
			.extracting("serialNumber", "status")
			.containsExactly(tuple("J333CCC", "FAILED"));
		assertThat(response.getCounts().getTotal()).isEqualTo(900);
		assertThat(response.getCounts().getSent()).isEqualTo(897);
		assertThat(response.getCounts().getFailed()).isEqualTo(3);
		assertThat(response.getMetadata().getTotalRecords()).isEqualTo(3);
	}

	@Test
	void toEndOfLeaseBatchStatusResponseAnswersWithAnEmptyListForABatchWithNoComputers() {
		final var batch = batchEntity("8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c", EXTERNAL_BATCH_ID, null);

		final var response = toEndOfLeaseBatchStatusResponse(batch, new PageImpl<>(List.<EndOfLeaseComputerEntity>of(), PageRequest.of(0, 100), 0), List.of());

		assertThat(response.getCounts().getTotal()).isZero();
		assertThat(response.getComputers()).isEmpty();
		assertThat(response.getMetadata().getTotalRecords()).isZero();
	}

	private static EndOfLeaseBatchEntity batchEntity(final String id, final String externalBatchId, final OffsetDateTime created) {
		return EndOfLeaseBatchEntity.create()
			.withId(id)
			.withExternalBatchId(externalBatchId)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withCreated(created);
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
}
