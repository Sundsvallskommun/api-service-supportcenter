package se.sundsvall.supportcenter.service.mapper;

import java.util.ArrayList;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputer;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;

import static java.util.stream.Collectors.toCollection;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;

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
			.withStatus(PENDING);
	}
}
