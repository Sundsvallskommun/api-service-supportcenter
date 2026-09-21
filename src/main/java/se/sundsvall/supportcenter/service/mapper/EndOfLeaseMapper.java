package se.sundsvall.supportcenter.service.mapper;

import generated.client.pob.PobPayload;
import generated.client.sysman.SaveMessagesToTargetsCommand;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import se.sundsvall.dept44.models.api.paging.PagingMetaData;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.EndOfLeaseBatchStatistics;
import se.sundsvall.supportcenter.api.model.EndOfLeaseBatchStatusResponse;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputer;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputerCounts;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputerStatus;
import se.sundsvall.supportcenter.api.model.EndOfLeaseStatisticsResponse;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchStatusCount;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatusCount;

import static generated.client.sysman.SaveMessagesToTargetsCommand.TargetTypeEnum.COMPUTER;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.EXCLUDED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.FAILED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.SENT;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_MUNICIPALITY;
import static se.sundsvall.supportcenter.service.mapper.constant.EndOfLeaseMapperConstants.EXCLUDED_ASSET_TAG_PREFIXES;

public final class EndOfLeaseMapper {

	/**
	 * The width of end_of_lease_computer.error_message, read off the column rather than written again here. The
	 * migration, the entity and this cut all have to agree, and a literal in three places agrees only until one of them
	 * is widened.
	 */
	private static final int ERROR_MESSAGE_LENGTH = EndOfLeaseComputerEntity.ERROR_MESSAGE_LENGTH;

	private EndOfLeaseMapper() {}

	public static EndOfLeaseBatchEntity toEndOfLeaseBatchEntity(final String municipalityId, final CreateEndOfLeaseBatchRequest createEndOfLeaseBatchRequest) {
		final var endOfLeaseBatchEntity = EndOfLeaseBatchEntity.create()
			.withExternalBatchId(createEndOfLeaseBatchRequest.getExternalBatchId())
			.withMunicipalityId(municipalityId);

		return endOfLeaseBatchEntity.withComputers(createEndOfLeaseBatchRequest.getComputers().stream()
			.map(endOfLeaseComputer -> toEndOfLeaseComputerEntity(endOfLeaseBatchEntity, endOfLeaseComputer))
			.collect(toCollection(ArrayList::new)));
	}

	/**
	 * The municipality of the computer a POB lookup answered with, which is what decides its SysMan installation.
	 *
	 * @param  configurationItems what POB answered the serial number with
	 * @return                    the municipality id, or null when POB knows no such computer or holds a municipality we
	 *                            cannot route on
	 */
	public static String toAssetMunicipalityId(final List<PobPayload> configurationItems) {
		return ofNullable(configurationItems).orElse(emptyList()).stream()
			.findFirst()
			.map(PobPayload::getData)
			.map(data -> data.get(KEY_MUNICIPALITY))
			// POB holds Data as name to object, and the municipality comes back as a string in every payload we have
			// seen. A cast would turn the one that does not into a ClassCastException, which spends the computer's
			// attempt on a value that was perfectly routable written out.
			.map(String::valueOf)
			.map(CommonMapper::toMunicipalityId)
			.orElse(null);
	}

	/**
	 * The call that asks one SysMan installation to send the message to a group of computers.
	 *
	 * A computer is known to SysMan by its asset tag, which is its computer name there, so that is what goes in targets.
	 * targetAll is false because the targets are named one by one, and leaving it out would let the message reach every
	 * computer in the installation.
	 *
	 * @param  computers the computers of one municipality
	 * @param  messageId the message to send
	 * @return           the command
	 */
	public static SaveMessagesToTargetsCommand toSaveMessagesToTargetsCommand(final List<EndOfLeaseComputerEntity> computers, final long messageId) {
		return new SaveMessagesToTargetsCommand()
			.targets(computers.stream()
				.map(EndOfLeaseComputerEntity::getAssetTag)
				// The same computer can legitimately sit in more than one batch, and naming it twice in one call asks
				// the installation to do the same work twice. Both rows still end up SENT, which is right: the row
				// records that this computer was told, and it was.
				.distinct()
				.toList())
			.messagesToSend(List.of(messageId))
			.targetType(COMPUTER)
			.targetAll(false);
	}

	/**
	 * A failure reason cut to what its column holds.
	 *
	 * POB and SysMan both answer a failure with whatever their own stack had to say, and a server that hands back a
	 * stack trace produces a message several times wider than the column. Stored as it comes, the insert is rejected
	 * from inside the very catch block that was recording the failure, which takes the whole run down and leaves the
	 * row to do it again on the next one.
	 *
	 * @param  errorMessage the reason, of any length
	 * @return              the reason, no wider than the column
	 */
	public static String toErrorMessage(final String errorMessage) {
		if (errorMessage == null || errorMessage.length() <= ERROR_MESSAGE_LENGTH) {
			return errorMessage;
		}
		return errorMessage.substring(0, ERROR_MESSAGE_LENGTH);
	}

	private static EndOfLeaseComputerEntity toEndOfLeaseComputerEntity(final EndOfLeaseBatchEntity endOfLeaseBatchEntity, final EndOfLeaseComputer endOfLeaseComputer) {
		return EndOfLeaseComputerEntity.create()
			.withBatch(endOfLeaseBatchEntity)
			.withSerialNumber(endOfLeaseComputer.getSerialNumber())
			.withAssetTag(endOfLeaseComputer.getAssetTag())
			.withEndOfLeaseDate(endOfLeaseComputer.getEndOfLeaseDate())
			.withStatus(toStatus(endOfLeaseComputer.getAssetTag()));
	}

	private static EndOfLeaseStatus toStatus(final String assetTag) {
		if (EXCLUDED_ASSET_TAG_PREFIXES.contains(toAssetTagPrefix(assetTag))) {
			return EXCLUDED;
		}
		return PENDING;
	}

	/**
	 * The letters an asset tag starts with, which is the computer name, so PUB12345 gives PUB. Empty for a tag that
	 * starts with anything else, and such a tag is sent like any other rather than held back on a guess.
	 */
	private static String toAssetTagPrefix(final String assetTag) {
		return assetTag.replaceAll("[^\\p{Alpha}].*", "").toUpperCase(Locale.ROOT);
	}

	/**
	 * The counts a municipality's batches add up to over a window, and the same counts for one page of those batches.
	 *
	 * The window counts and the page counts come from two queries and answer two different questions. The window counts
	 * say what every batch between the two days adds up to, so that a reader on page four still sees what the page is a
	 * part of. The page counts say what each batch on the page holds. Adding the page up would answer the first question
	 * with one page of the second.
	 *
	 * The batches of the page decide which rows appear and in which order, and the counts are looked up against them. A
	 * batch is registered with at least one computer, so a batch with no counts should not happen, but one would answer
	 * with zeroes rather than be dropped from a page that has already been counted.
	 *
	 * The window is carried into the answer rather than left to the caller to remember, so that a reader can tell a
	 * quiet month from a quiet service without knowing what was asked for.
	 *
	 * @param  batches           the page of batches inside the window, newest first
	 * @param  countsOfThePage   one count per batch and state, for the batches of the page
	 * @param  countsOfTheWindow one count per state, for every batch inside the window
	 * @param  from              the first day the counts cover
	 * @param  to                the last day the counts cover
	 * @return                   the statistics
	 */
	public static EndOfLeaseStatisticsResponse toEndOfLeaseStatisticsResponse(
		final Page<EndOfLeaseBatchEntity> batches,
		final List<EndOfLeaseBatchStatusCount> countsOfThePage,
		final List<EndOfLeaseStatusCount> countsOfTheWindow,
		final LocalDate from,
		final LocalDate to) {

		final var countsByBatchId = countsOfThePage.stream()
			.collect(groupingBy(EndOfLeaseBatchStatusCount::batchId));

		return EndOfLeaseStatisticsResponse.create()
			.withFrom(from)
			.withTo(to)
			.withCounts(toEndOfLeaseComputerCounts(countsOfTheWindow))
			.withBatches(batches.getContent().stream()
				.map(batch -> toEndOfLeaseBatchStatistics(batch, countsByBatchId.getOrDefault(batch.getId(), emptyList())))
				.toList())
			.withMetadata(toPagingMetaData(batches));
	}

	/**
	 * One batch, one page of the computers in it that were asked for, and what the whole batch adds up to.
	 *
	 * The counts come from a query of their own rather than from the page, because they have to cover the whole batch
	 * whichever states were asked for and whichever page is being read. That is the difference between three computers
	 * failing and three out of nine hundred failing, and a page of ten cannot answer it.
	 *
	 * The states are picked out in the query rather than here. Filtering a page after it has been cut out of the batch
	 * would answer a page of ten with however few of those ten happened to match.
	 *
	 * @param  endOfLeaseBatchEntity the batch
	 * @param  computers             the page of computers that were asked for
	 * @param  countsOfTheBatch      one count per state, for the whole batch
	 * @return                       the batch with the page of computers
	 */
	public static EndOfLeaseBatchStatusResponse toEndOfLeaseBatchStatusResponse(
		final EndOfLeaseBatchEntity endOfLeaseBatchEntity,
		final Page<EndOfLeaseComputerEntity> computers,
		final List<EndOfLeaseBatchStatusCount> countsOfTheBatch) {

		return EndOfLeaseBatchStatusResponse.create()
			.withId(endOfLeaseBatchEntity.getId())
			.withExternalBatchId(endOfLeaseBatchEntity.getExternalBatchId())
			.withCreated(endOfLeaseBatchEntity.getCreated())
			.withCounts(toEndOfLeaseComputerCounts(toStatusCounts(countsOfTheBatch)))
			.withComputers(computers.getContent().stream()
				.map(EndOfLeaseMapper::toEndOfLeaseComputerStatus)
				.toList())
			.withMetadata(toPagingMetaData(computers));
	}

	private static EndOfLeaseBatchStatistics toEndOfLeaseBatchStatistics(final EndOfLeaseBatchEntity batch, final List<EndOfLeaseBatchStatusCount> countsOfOneBatch) {
		return EndOfLeaseBatchStatistics.create()
			.withId(batch.getId())
			.withExternalBatchId(batch.getExternalBatchId())
			.withCreated(batch.getCreated())
			.withCounts(toEndOfLeaseComputerCounts(toStatusCounts(countsOfOneBatch)));
	}

	/**
	 * Folds the rows of a grouped count into the five numbers the API answers with.
	 *
	 * The total is every row added up rather than the four states added together, so that a state added to the enum and
	 * forgotten here is missing from its own field but still counted in the total, instead of making the total disagree
	 * with the rows it came from.
	 */
	private static EndOfLeaseComputerCounts toEndOfLeaseComputerCounts(final List<EndOfLeaseStatusCount> counts) {
		final var byStatus = new EnumMap<EndOfLeaseStatus, Long>(EndOfLeaseStatus.class);
		counts.forEach(count -> byStatus.merge(count.status(), count.count(), Long::sum));

		return EndOfLeaseComputerCounts.create()
			.withTotal(byStatus.values().stream().mapToLong(Long::longValue).sum())
			.withPending(byStatus.getOrDefault(PENDING, 0L))
			.withSent(byStatus.getOrDefault(SENT, 0L))
			.withFailed(byStatus.getOrDefault(FAILED, 0L))
			.withExcluded(byStatus.getOrDefault(EXCLUDED, 0L));
	}

	private static List<EndOfLeaseStatusCount> toStatusCounts(final List<EndOfLeaseBatchStatusCount> counts) {
		return counts.stream()
			.map(count -> new EndOfLeaseStatusCount(count.status(), count.count()))
			.toList();
	}

	/**
	 * The page number is one based in the API and zero based in Spring Data, which is the whole reason this is not
	 * {@code new PagingMetaData()} with the page copied straight across.
	 */
	private static PagingMetaData toPagingMetaData(final Page<?> page) {
		return PagingMetaData.create()
			.withPage(page.getNumber() + 1)
			.withLimit(page.getSize())
			.withCount(page.getNumberOfElements())
			.withTotalRecords(page.getTotalElements())
			.withTotalPages(page.getTotalPages());
	}

	private static EndOfLeaseComputerStatus toEndOfLeaseComputerStatus(final EndOfLeaseComputerEntity endOfLeaseComputerEntity) {
		return EndOfLeaseComputerStatus.create()
			.withSerialNumber(endOfLeaseComputerEntity.getSerialNumber())
			.withAssetTag(endOfLeaseComputerEntity.getAssetTag())
			.withStatus(endOfLeaseComputerEntity.getStatus().name())
			.withAttempts(endOfLeaseComputerEntity.getAttempts())
			.withErrorMessage(endOfLeaseComputerEntity.getErrorMessage())
			.withSentAt(endOfLeaseComputerEntity.getSentAt());
	}
}
