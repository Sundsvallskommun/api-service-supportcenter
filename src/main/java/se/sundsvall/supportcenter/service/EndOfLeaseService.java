package se.sundsvall.supportcenter.service;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseBatchRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchEntity;

import static se.sundsvall.supportcenter.service.mapper.EndOfLeaseMapper.toEndOfLeaseBatchEntity;

@Service
public class EndOfLeaseService {

	private static final Logger LOG = LoggerFactory.getLogger(EndOfLeaseService.class);

	private final EndOfLeaseBatchRepository endOfLeaseBatchRepository;

	public EndOfLeaseService(final EndOfLeaseBatchRepository endOfLeaseBatchRepository) {
		this.endOfLeaseBatchRepository = endOfLeaseBatchRepository;
	}

	/**
	 * Stores a batch with every computer in it waiting to be sent.
	 *
	 * A batch sent again under an id the sender has already used is answered with the id of the batch stored the first
	 * time, and nothing is queued a second time. That is what lets a sender who timed out without seeing our answer retry
	 * without every computer in the batch being reported to SysMan twice.
	 *
	 * @param  municipalityId               the municipality of the sender
	 * @param  createEndOfLeaseBatchRequest the batch to store
	 * @return                              the id of the stored batch
	 */
	public String registerBatch(final String municipalityId, final CreateEndOfLeaseBatchRequest createEndOfLeaseBatchRequest) {
		final var externalBatchId = createEndOfLeaseBatchRequest.getExternalBatchId();
		final var existingBatchId = findBatchId(municipalityId, externalBatchId);

		existingBatchId.ifPresent(batchId -> logResend(batchId, externalBatchId, createEndOfLeaseBatchRequest.getComputers().size()));

		return existingBatchId.orElseGet(() -> store(municipalityId, createEndOfLeaseBatchRequest));
	}

	private Optional<String> findBatchId(final String municipalityId, final String externalBatchId) {
		return endOfLeaseBatchRepository.findByMunicipalityIdAndExternalBatchId(municipalityId, externalBatchId)
			.map(EndOfLeaseBatchEntity::getId);
	}

	/**
	 * Says that a batch arrived again, and says it louder when the batch is not the one that was stored. Only the id
	 * decides whether something is a resend, so a sender that reuses an id for a different set of computers has those
	 * computers dropped. The count is the cheapest signal that this happened.
	 */
	private void logResend(final String batchId, final String externalBatchId, final int resentComputers) {
		final var storedComputers = endOfLeaseBatchRepository.countComputers(batchId);

		if (storedComputers == resentComputers) {
			LOG.info("Batch with external id {} is already registered as {} with {} computers, nothing was stored", externalBatchId, batchId, storedComputers);
		} else {
			LOG.warn("Batch with external id {} is already registered as {} with {} computers, but arrived again with {}. The computers that arrived now were not stored",
				externalBatchId, batchId, storedComputers, resentComputers);
		}
	}

	private String store(final String municipalityId, final CreateEndOfLeaseBatchRequest createEndOfLeaseBatchRequest) {
		try {
			return endOfLeaseBatchRepository.save(toEndOfLeaseBatchEntity(municipalityId, createEndOfLeaseBatchRequest)).getId();
		} catch (final DataIntegrityViolationException e) {
			// A resend that arrives while the first one is still being stored loses the race on the unique index instead
			// of on the lookup above. The batch is registered either way, so the sender is given the same answer.
			return recoverFromDuplicate(municipalityId, createEndOfLeaseBatchRequest.getExternalBatchId(), e);
		}
	}

	private String recoverFromDuplicate(final String municipalityId, final String externalBatchId, final DataIntegrityViolationException cause) {
		try {
			return findBatchId(municipalityId, externalBatchId).orElseThrow(() -> cause);
		} catch (final RuntimeException e) {
			// Whatever went wrong while looking the batch up says nothing about which constraint was violated, and that
			// is the part an operator needs, so the two are reported together rather than one replacing the other.
			if (e != cause) {
				e.addSuppressed(cause);
			}
			throw e;
		}
	}
}
