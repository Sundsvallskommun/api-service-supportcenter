package se.sundsvall.supportcenter.service.mapper;

import java.util.ArrayList;
import java.util.Locale;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputer;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus;

import static java.util.stream.Collectors.toCollection;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.EXCLUDED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;
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
