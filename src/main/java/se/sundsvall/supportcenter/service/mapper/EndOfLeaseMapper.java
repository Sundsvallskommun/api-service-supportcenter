package se.sundsvall.supportcenter.service.mapper;

import generated.client.pob.PobPayload;
import generated.client.sysman.SaveMessagesToTargetsCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputer;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus;

import static generated.client.sysman.SaveMessagesToTargetsCommand.TargetTypeEnum.COMPUTER;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.EXCLUDED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;
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
}
