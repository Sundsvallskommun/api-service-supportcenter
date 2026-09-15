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
			.map(data -> (String) data.get(KEY_MUNICIPALITY))
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
				.toList())
			.messagesToSend(List.of(messageId))
			.targetType(COMPUTER)
			.targetAll(false);
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
	 * The letters an asset tag starts with, which is the computer name. Empty for a tag that starts with anything else,
	 * and such a tag is sent like any other rather than being held back on a guess.
	 * "[^\p{Alpha}]" matches a sign that is not a character. ".*" matches everything after.
	 * e.g. PUB12345, the matching will start with 1 and go over 12345, left is "PUB", which will be returned .
	 */
	private static String toAssetTagPrefix(final String assetTag) {
		return assetTag.replaceAll("[^\\p{Alpha}].*", "").toUpperCase(Locale.ROOT);
	}
}
