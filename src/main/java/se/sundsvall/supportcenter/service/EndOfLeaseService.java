package se.sundsvall.supportcenter.service;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.RetryEndOfLeaseComputersRequest;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseBatchRepository;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseComputerRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;

import static java.util.Objects.isNull;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.FAILED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;
import static se.sundsvall.supportcenter.service.mapper.EndOfLeaseMapper.toEndOfLeaseBatchEntity;

@Service
public class EndOfLeaseService {

	private static final Logger LOG = LoggerFactory.getLogger(EndOfLeaseService.class);

	private final EndOfLeaseBatchRepository endOfLeaseBatchRepository;
	private final EndOfLeaseComputerRepository endOfLeaseComputerRepository;

	public EndOfLeaseService(
		final EndOfLeaseBatchRepository endOfLeaseBatchRepository,
		final EndOfLeaseComputerRepository endOfLeaseComputerRepository) {

		this.endOfLeaseBatchRepository = endOfLeaseBatchRepository;
		this.endOfLeaseComputerRepository = endOfLeaseComputerRepository;
	}

	/**
	 * Puts computers that were given up on back in the queue.
	 *
	 * FAILED is where a computer ends up once its attempts are gone, and nothing in the two runs can take it further.
	 * Without this the only way back is an UPDATE written by hand, and the queue health indicator counts every such row
	 * for as long as it is there, so one computer POB has never heard of leaves the service RESTRICTED for good.
	 *
	 * The attempts are reset along with the state, or a computer would be given up on again by the first run that
	 * reached it. The POB municipality is deliberately left as it is: a computer that never got past the lookup has
	 * none and goes back to the lookup run, and one that did keeps it and goes straight to the dispatch run.
	 *
	 * @param  municipalityId                  the municipality of the sender that registered the batch
	 * @param  retryEndOfLeaseComputersRequest the computers to take, or nothing to take all of them
	 * @return                                 the number of computers that were put back in the queue
	 */
	public int retryComputersGivenUpOn(final String municipalityId, final RetryEndOfLeaseComputersRequest retryEndOfLeaseComputersRequest) {
		final var computers = findComputersGivenUpOn(municipalityId, retryEndOfLeaseComputersRequest.getSerialNumbers());

		computers.forEach(computer -> {
			computer.setStatus(PENDING);
			computer.setAttempts(0);
			computer.setErrorMessage(null);
			// Any hold from the outage that gave up on it is over as far as the person asking is concerned.
			computer.setRetryAfter(null);
		});

		endOfLeaseComputerRepository.saveAll(computers);

		LOG.info("Put {} computer(s) of municipality {} back in the queue", computers.size(), sanitizeForLogging(municipalityId));

		return computers.size();
	}

	private List<EndOfLeaseComputerEntity> findComputersGivenUpOn(final String municipalityId, final List<String> serialNumbers) {
		if (isNull(serialNumbers) || serialNumbers.isEmpty()) {
			return endOfLeaseComputerRepository.findByStatusAndBatchMunicipalityId(FAILED, municipalityId);
		}
		return endOfLeaseComputerRepository.findByStatusAndBatchMunicipalityIdAndSerialNumberIn(FAILED, municipalityId, serialNumbers);
	}

	/**
	 * Stores a batch with every computer in it waiting to be sent.
	 *
	 * A batch sent again under an id the sender has already used is answered with the id of the batch stored the first
	 * time, and nothing is queued a second time. That is what lets a sender who timed out without seeing our answer retry
	 * without every computer in the batch being sent its message twice.
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

		final var sanitizedBatchId = sanitizeForLogging(externalBatchId);

		if (storedComputers == resentComputers) {
			LOG.info("Batch with external id {} is already registered as {} with {} computers, nothing was stored", sanitizedBatchId, batchId, storedComputers);
		} else {
			LOG.warn("Batch with external id {} is already registered as {} with {} computers, but arrived again with {}. The computers that arrived now were not stored",
				sanitizedBatchId, batchId, storedComputers, resentComputers);
		}
	}

	/**
	 * Deliberately not transactional, and must not be called from something that is. The recovery below depends on the
	 * failed save having been its own committed transaction: inside a caller's transaction the lookup would read the
	 * snapshot taken before the competing insert committed and find nothing, and the transaction would already be marked
	 * rollback-only by the failed flush.
	 */
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
		final Optional<String> batchId;
		try {
			batchId = findBatchId(municipalityId, externalBatchId);
		} catch (final RuntimeException e) {
			// Whatever went wrong while looking the batch up says nothing about which constraint was violated, and that
			// is the part an operator needs, so the two are reported together rather than one replacing the other.
			e.addSuppressed(cause);
			throw e;
		}

		// The driver logs the violation as a bare duplicate entry warning, which reads like the database being unwell
		// rather than a sender being answered correctly. This is the line that says which of the two it was.
		batchId.ifPresent(id -> LOG.info("Batch with external id {} arrived again while the first one was still being stored, and lost on the unique index. It is registered as {}, nothing was stored",
			sanitizeForLogging(externalBatchId), id));

		return batchId.orElseThrow(() -> cause);
	}
}
