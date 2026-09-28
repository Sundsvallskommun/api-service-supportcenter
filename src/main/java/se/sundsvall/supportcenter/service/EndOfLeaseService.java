package se.sundsvall.supportcenter.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.EndOfLeaseBatchStatusResponse;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputerParameters;
import se.sundsvall.supportcenter.api.model.EndOfLeaseStatisticsParameters;
import se.sundsvall.supportcenter.api.model.EndOfLeaseStatisticsResponse;
import se.sundsvall.supportcenter.api.model.RetryEndOfLeaseComputersRequest;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseBatchRepository;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseComputerRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchStatusCount;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus;

import static java.time.ZoneId.systemDefault;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
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
	 * What every batch a municipality has registered inside a window adds up to, and the same counts one page of those
	 * batches at a time.
	 *
	 * This is the state the rows are in now, not a record of how they got there. A computer that failed twice and then
	 * went through counts as sent, and the reason it failed is gone, since every run that succeeds clears it. What is
	 * left of a struggle that ended well is the attempts on the computer itself, which the batch answers with.
	 *
	 * The counts cover a window of days rather than everything ever stored. Left out, the window is the month up to
	 * today. Given, it is what was asked for. A caller who names only the first day is counted up to today, and one who
	 * names only the last day is counted a month back from it.
	 *
	 * A wide window costs a count the database takes over more rows. It does not cost a larger answer: the batches come
	 * back one page at a time and the window counts are four numbers however many batches they were taken over.
	 *
	 * The days are read in the zone the rows were written in, which is the one the two runs stamp their timestamps in.
	 *
	 * The three queries are read in one transaction. The session is not held open for the request, so without it each
	 * would see the table as it was when it ran, and a write landing between two of them would answer the window from
	 * one moment and the batches from another. The retry endpoint and the dispatch run both write while this is read.
	 *
	 * @param  municipalityId the municipality of the sender that registered the batches
	 * @param  parameters     the window to count and the page of batches to answer with
	 * @return                the counts, and the window they cover
	 * @throws Problem        BAD_REQUEST when the window ends before it starts
	 */
	@Transactional(readOnly = true)
	public EndOfLeaseStatisticsResponse getStatistics(final String municipalityId, final EndOfLeaseStatisticsParameters parameters) {
		final var lastDay = ofNullable(parameters.getTo()).orElseGet(() -> LocalDate.now(systemDefault()));
		final var firstDay = ofNullable(parameters.getFrom()).orElseGet(() -> lastDay.minus(DEFAULT_WINDOW));

		if (firstDay.isAfter(lastDay)) {
			throw Problem.valueOf(BAD_REQUEST, WINDOW_ENDS_BEFORE_IT_STARTS.formatted(firstDay, lastDay));
		}

		// The last day is counted in full, so the queries stop at the first moment of the day after it.
		final var from = firstDay.atStartOfDay(systemDefault()).toOffsetDateTime();
		final var to = lastDay.plusDays(1).atStartOfDay(systemDefault()).toOffsetDateTime();

		final var batches = endOfLeaseBatchRepository.findInWindow(municipalityId, from, to, PageRequest.of(parameters.getPage() - 1, parameters.getLimit()));
		final var batchIds = batches.getContent().stream()
			.map(EndOfLeaseBatchEntity::getId)
			.toList();

		// A page past the last one holds no batches, and there is nothing to count them against.
		final var countsOfThePage = batchIds.isEmpty() ? Collections.<EndOfLeaseBatchStatusCount>emptyList() : endOfLeaseComputerRepository.countByStatusGroupedByBatch(batchIds);

		return toEndOfLeaseStatisticsResponse(batches, countsOfThePage, endOfLeaseComputerRepository.countByStatusInWindow(municipalityId, from, to), firstDay, lastDay);
	}

	/**
	 * One batch, one page of the computers in it, and why the ones that were given up on were.
	 *
	 * The counts cover the whole batch whichever states are asked for and whichever page is being read, so that a handful
	 * of failures is read against the size of the batch they came out of rather than on their own. They are counted by
	 * the database rather than taken from the page, which is the only way a page of ten can say what it is a part of.
	 *
	 * The page and the counts are read in one transaction, or a computer could be listed in the state it was in before
	 * a write and counted in the state it was in after one, and the page would not be readable against the counts at
	 * all.
	 *
	 * @param  municipalityId the municipality of the sender that registered the batch
	 * @param  batchId        the id of the batch
	 * @param  parameters     the states to list and the page of computers to answer with
	 * @return                the batch with the page of computers that were asked for
	 * @throws Problem        NOT_FOUND when the municipality has no batch under the id
	 */
	@Transactional(readOnly = true)
	public EndOfLeaseBatchStatusResponse getBatchStatus(final String municipalityId, final String batchId, final EndOfLeaseComputerParameters parameters) {
		final var batch = endOfLeaseBatchRepository.findByIdAndMunicipalityId(batchId, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, BATCH_NOT_FOUND.formatted(batchId, municipalityId)));

		final var computers = endOfLeaseComputerRepository.findByBatchId(batchId, toStatuses(parameters.getStatus()), PageRequest.of(parameters.getPage() - 1, parameters.getLimit()));

		return toEndOfLeaseBatchStatusResponse(batch, computers, endOfLeaseComputerRepository.countByStatusGroupedByBatch(List.of(batchId)));
	}

	/**
	 * The states to read. Nothing asked for is every state, which is what a caller who left the parameter out means by
	 * it, and what an empty collection would not mean to the query.
	 *
	 * The names are turned into enum constants here rather than compared as strings in the query, so that the state a
	 * caller asked for and the state the column holds cannot differ by case or by spelling. {@code @OneOf} on the
	 * parameter has already turned anything else away.
	 */
	private static Collection<EndOfLeaseStatus> toStatuses(final List<String> statuses) {
		if (isNull(statuses) || statuses.isEmpty()) {
			return EnumSet.allOf(EndOfLeaseStatus.class);
		}

		return statuses.stream()
			.map(EndOfLeaseStatus::valueOf)
			.collect(toCollection(() -> EnumSet.noneOf(EndOfLeaseStatus.class)));
	}
}
