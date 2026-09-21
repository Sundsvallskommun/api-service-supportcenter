package se.sundsvall.supportcenter.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.EndOfLeaseBatchStatusResponse;
import se.sundsvall.supportcenter.api.model.EndOfLeaseStatisticsResponse;
import se.sundsvall.supportcenter.api.model.RetryEndOfLeaseComputersRequest;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseBatchRepository;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseComputerRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;

import static java.time.ZoneId.systemDefault;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.CONFLICT;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.FAILED;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;
import static se.sundsvall.supportcenter.service.mapper.EndOfLeaseMapper.toEndOfLeaseBatchEntity;
import static se.sundsvall.supportcenter.service.mapper.EndOfLeaseMapper.toEndOfLeaseBatchStatusResponse;
import static se.sundsvall.supportcenter.service.mapper.EndOfLeaseMapper.toEndOfLeaseStatisticsResponse;

@Service
public class EndOfLeaseService {

	private static final Logger LOG = LoggerFactory.getLogger(EndOfLeaseService.class);
	private static final String ALREADY_REGISTERED = "A batch with external id '%s' is already registered as '%s'";

	private static final String BATCH_NOT_FOUND = "No batch with id '%s' is registered for municipality '%s'";
	private static final String WINDOW_ENDS_BEFORE_IT_STARTS = "The window ends before it starts: from '%s' is later than to '%s'";

	/**
	 * How far back the counts reach when nobody says. Long enough to hold a month of daily batches, and short enough
	 * that the read stays the same size as the service ages.
	 */
	private static final Period DEFAULT_WINDOW = Period.ofMonths(1);

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
	 * A batch sent again under an id the sender has already used is rejected with 409 Conflict, and nothing is queued a
	 * second time. The detail names the batch stored the first time, so a sender who timed out without seeing our answer
	 * and retries can tell that the batch arrived.
	 *
	 * @param  municipalityId               the municipality of the sender
	 * @param  createEndOfLeaseBatchRequest the batch to store
	 * @return                              the id of the stored batch
	 */
	public String registerBatch(final String municipalityId, final CreateEndOfLeaseBatchRequest createEndOfLeaseBatchRequest) {
		final var externalBatchId = createEndOfLeaseBatchRequest.getExternalBatchId();

		findBatchId(municipalityId, externalBatchId).ifPresent(batchId -> {
			LOG.info("Batch with external id {} is already registered as {}, nothing was stored", sanitizeForLogging(externalBatchId), batchId);
			throw alreadyRegistered(externalBatchId, batchId);
		});

		return store(municipalityId, createEndOfLeaseBatchRequest);
	}

	private static ThrowableProblem alreadyRegistered(final String externalBatchId, final String batchId) {
		return Problem.valueOf(CONFLICT, ALREADY_REGISTERED.formatted(externalBatchId, batchId));
	}

	private Optional<String> findBatchId(final String municipalityId, final String externalBatchId) {
		return endOfLeaseBatchRepository.findByMunicipalityIdAndExternalBatchId(municipalityId, externalBatchId)
			.map(EndOfLeaseBatchEntity::getId);
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
			throw explainDuplicate(municipalityId, createEndOfLeaseBatchRequest.getExternalBatchId(), e);
		}
	}

	private RuntimeException explainDuplicate(final String municipalityId, final String externalBatchId, final DataIntegrityViolationException cause) {
		final Optional<String> batchId;
		try {
			batchId = findBatchId(municipalityId, externalBatchId);
		} catch (final RuntimeException e) {
			// Whatever went wrong while looking the batch up says nothing about which constraint was violated, and that
			// is the part an operator needs, so the two are reported together rather than one replacing the other.
			e.addSuppressed(cause);
			throw e;
		}

		if (batchId.isEmpty()) {
			return cause;
		}

		// The driver logs the violation as a bare duplicate entry warning, which reads like the database being unwell
		// rather than a sender being answered correctly. This is the line that says which of the two it was.
		final var sanitizedExternalBatchId = sanitizeForLogging(externalBatchId);
		LOG.info("Batch with external id {} arrived again while the first one was still being stored, and lost on the unique index. " +
			"It is registered as {}, nothing was stored", sanitizedExternalBatchId, batchId.get());

		return alreadyRegistered(externalBatchId, batchId.get());
	}

	/**
	 * What every batch a municipality has registered adds up to, and the same counts one batch at a time.
	 *
	 * This is the state the rows are in now, not a record of how they got there. A computer that failed twice and then
	 * went through counts as sent, and the reason it failed is gone, since every run that succeeds clears it. What is
	 * left of a struggle that ended well is the attempts on the computer itself, which the batch answers with.
	 *
	 * The counts cover a window of days rather than everything ever stored, since counting every state of every batch
	 * reads every computer row of the municipality and nothing ever leaves the table. Left out, the window is the month
	 * up to today. Given, it is what was asked for, and asking for a wide one is a deliberate act that pays for itself.
	 * A caller who names only one end gets the default span measured from that end, so there is no way to ask for a
	 * window without one.
	 *
	 * The days are read in the zone the rows were written in, which is the one the two runs stamp their timestamps in.
	 *
	 * @param  municipalityId the municipality of the sender that registered the batches
	 * @param  from           the first day to count, or nothing for a month back from the last day
	 * @param  to             the last day to count, counted in full, or nothing for today
	 * @return                the counts, and the window they cover
	 * @throws Problem        BAD_REQUEST when the window ends before it starts
	 */
	public EndOfLeaseStatisticsResponse getStatistics(final String municipalityId, final LocalDate from, final LocalDate to) {
		final var lastDay = ofNullable(to).orElseGet(() -> LocalDate.now(systemDefault()));
		final var firstDay = ofNullable(from).orElseGet(() -> lastDay.minus(DEFAULT_WINDOW));

		if (firstDay.isAfter(lastDay)) {
			throw Problem.valueOf(BAD_REQUEST, WINDOW_ENDS_BEFORE_IT_STARTS.formatted(firstDay, lastDay));
		}

		// The last day is counted in full, so the query stops at the first moment of the day after it.
		final var counts = endOfLeaseComputerRepository.countByStatusGroupedByBatch(municipalityId,
			firstDay.atStartOfDay(systemDefault()).toOffsetDateTime(),
			lastDay.plusDays(1).atStartOfDay(systemDefault()).toOffsetDateTime());

		return toEndOfLeaseStatisticsResponse(counts, firstDay, lastDay);
	}

	/**
	 * One batch with the computers in it, the state each one is in, and why the ones that were given up on were.
	 *
	 * The counts cover the whole batch whichever states are asked for, so that a handful of failures is read against
	 * the size of the batch they came out of rather than on their own.
	 *
	 * @param  municipalityId the municipality of the sender that registered the batch
	 * @param  batchId        the id of the batch
	 * @param  statuses       the states to list, or nothing to list them all
	 * @return                the batch with the computers that were asked for
	 * @throws Problem        NOT_FOUND when the municipality has no batch under the id
	 */
	public EndOfLeaseBatchStatusResponse getBatchStatus(final String municipalityId, final String batchId, final List<String> statuses) {
		final var batch = endOfLeaseBatchRepository.findByIdAndMunicipalityId(batchId, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, BATCH_NOT_FOUND.formatted(batchId, municipalityId)));

		return toEndOfLeaseBatchStatusResponse(batch, endOfLeaseComputerRepository.findByBatchId(batchId), statuses);
	}
}
