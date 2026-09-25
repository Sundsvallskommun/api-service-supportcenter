package se.sundsvall.supportcenter.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchStatusCount;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatusCount;

@CircuitBreaker(name = "endOfLeaseComputerRepository")
public interface EndOfLeaseComputerRepository extends JpaRepository<EndOfLeaseComputerEntity, String> {

	/**
	 * Redeclared rather than inherited so that the circuit breaker on this interface covers it, the same way
	 * {@link EndOfLeaseBatchRepository#save} is. Measured against this configuration: a call to a method declared here
	 * is recorded by the breaker, and a call to one inherited from JpaRepository is not recorded at all. Left
	 * inherited, every write the two runs make would sit outside the breaker while every read sat inside it.
	 *
	 * No instance configuration accompanies this one. The sibling needs {@code ignoreExceptions} because a resent batch
	 * losing on the unique index is an expected outcome; nothing a computer row does is expected to fail, so the
	 * inherited defaults are what this breaker should have.
	 *
	 * @param  <S>    the type of the computer to store
	 * @param  entity the computer to store
	 * @return        the stored computer
	 */
	@Override
	<S extends EndOfLeaseComputerEntity> S save(S entity);

	/**
	 * Redeclared for the same reason as {@link #save}. Used by the retry endpoint, which puts a whole municipality's
	 * computers back in the queue in one call.
	 *
	 * @param  <S>      the type of the computers to store
	 * @param  entities the computers to store
	 * @return          the stored computers
	 */
	@Override
	<S extends EndOfLeaseComputerEntity> List<S> saveAll(Iterable<S> entities);

	/**
	 * The computers the lookup run has made ready to send, oldest first.
	 *
	 * Rows held back after a dependency failure are left out until their window is up. Such a failure costs a computer
	 * no attempt and nothing else moves it, so without this it keeps its place at the front of every page and a page
	 * worth of them stops everything behind them for good.
	 *
	 * @param  status   the state to look in
	 * @param  now      the moment to measure the hold against
	 * @param  pageable how many to take
	 * @return          the computers whose municipality is known and whose turn it is
	 */
	@Query("select computer from EndOfLeaseComputerEntity computer "
		+ "where computer.status = :status "
		+ "and computer.assetMunicipalityId is not null "
		+ "and (computer.retryAfter is null or computer.retryAfter <= :now) "
		+ "order by computer.created")
	List<EndOfLeaseComputerEntity> findReadyToSend(@Param("status") EndOfLeaseStatus status, @Param("now") OffsetDateTime now, Pageable pageable);

	/**
	 * The computers the lookup run has left to do, oldest first.
	 *
	 * A null municipality is what says a computer has not been looked up yet, so no second state is needed to tell the
	 * two runs apart. Taken a page at a time rather than all at once, since every computer here is one POB call and a
	 * run has to finish inside its lock.
	 *
	 * The hold is filtered on for symmetry with {@link #findReadyToSend}, though nothing sets one on a row awaiting
	 * lookup any more. That run answers a dependency failure by stopping, not by holding the page back.
	 *
	 * @param  status   the state to look in
	 * @param  now      the moment to measure the hold against
	 * @param  pageable how many to take
	 * @return          the computers waiting to be looked up whose turn it is
	 */
	@Query("select computer from EndOfLeaseComputerEntity computer "
		+ "where computer.status = :status "
		+ "and computer.assetMunicipalityId is null "
		+ "and (computer.retryAfter is null or computer.retryAfter <= :now) "
		+ "order by computer.created")
	List<EndOfLeaseComputerEntity> findAwaitingLookup(@Param("status") EndOfLeaseStatus status, @Param("now") OffsetDateTime now, Pageable pageable);

	/**
	 * The computer that has been waiting longest, whichever of the two runs it is waiting for.
	 *
	 * Read by the health indicator rather than by a run, so that a queue standing still is noticed even when nothing is
	 * picking it up. Served by ix_end_of_lease_computer_status_created without a sort.
	 *
	 * @param  status the state to look in
	 * @return        the oldest computer in the state, or empty when none is waiting
	 */
	Optional<EndOfLeaseComputerEntity> findFirstByStatusOrderByCreated(EndOfLeaseStatus status);

	/**
	 * The computers of a municipality that are in a state, whether or not they have been looked up yet.
	 *
	 * Scoped by the municipality of the batch rather than by the one read from POB, since a computer that never got as
	 * far as a lookup has no POB municipality at all, and those are exactly the ones somebody asks to retry.
	 *
	 * @param  status         the state to look in
	 * @param  municipalityId the municipality of the sender that registered the batch
	 * @return                the computers in the state
	 */
	List<EndOfLeaseComputerEntity> findByStatusAndBatchMunicipalityId(EndOfLeaseStatus status, String municipalityId);

	/**
	 * The same, narrowed to the computers a caller named.
	 *
	 * @param  status         the state to look in
	 * @param  municipalityId the municipality of the sender that registered the batch
	 * @param  serialNumbers  the serial numbers to take
	 * @return                the computers in the state that carry one of the serial numbers
	 */
	List<EndOfLeaseComputerEntity> findByStatusAndBatchMunicipalityIdAndSerialNumberIn(EndOfLeaseStatus status, String municipalityId, Collection<String> serialNumbers);

	/**
	 * How many computers are in a state.
	 *
	 * Read by the health indicator to count the ones that have been given up on. The scheduler indicator says so on the
	 * run that gives up, but the aspect resets it on the next run that goes well, so without a count read from the rows
	 * themselves a computer nobody can report is forgotten an hour later.
	 *
	 * @param  status the state to count
	 * @return        the number of computers in the state
	 */
	long countByStatus(EndOfLeaseStatus status);

	/**
	 * How many computers of each of the named batches are in each state.
	 *
	 * One row per batch and state rather than one query per batch. The caller names the batches, which is what bounds
	 * this: the statistics read hands it one page of batch ids and the single batch read hands it one id, so the number
	 * of rows that come back is four times the page and never grows with the table.
	 *
	 * Unordered, because nothing reads it in order. The rows are looked up by batch id against a page of batches that
	 * carries the order the answer keeps, so a sort here would be one the database pays for and nobody observes.
	 *
	 * @param  batchIds the ids of the batches to count
	 * @return          one count per batch and state
	 */
	@Query("select new se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchStatusCount("
		+ "computer.batch.id, computer.batch.externalBatchId, computer.batch.created, computer.status, count(computer)) "
		+ "from EndOfLeaseComputerEntity computer "
		+ "where computer.batch.id in :batchIds "
		+ "group by computer.batch.id, computer.batch.externalBatchId, computer.batch.created, computer.status")
	List<EndOfLeaseBatchStatusCount> countByStatusGroupedByBatch(@Param("batchIds") Collection<String> batchIds);

	/**
	 * How many computers a municipality registered inside a window are in each state, across every batch of the window.
	 *
	 * Scoped by the municipality of the sender, the same way the retry endpoint is, so that a computer POB places in
	 * another municipality still counts towards the batch it arrived in.
	 *
	 * Separate from the per batch counts because it has to cover the whole window and those cover one page of it. The
	 * window still decides how many rows the database reads, and a batch a day of a thousand computers is a third of a
	 * million rows a year, but the counting happens in the database and four rows come back. What the window saves here
	 * is a scan, not a heap.
	 *
	 * @param  municipalityId the municipality of the sender that registered the batches
	 * @param  from           the first moment to count, included
	 * @param  to             the moment to stop counting at, not included
	 * @return                one count per state
	 */
	@Query("select new se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatusCount(computer.status, count(computer)) "
		+ "from EndOfLeaseComputerEntity computer "
		+ "where computer.batch.municipalityId = :municipalityId "
		+ "and computer.batch.created >= :from "
		+ "and computer.batch.created < :to "
		+ "group by computer.status")
	List<EndOfLeaseStatusCount> countByStatusInWindow(@Param("municipalityId") String municipalityId, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

	/**
	 * One page of the computers of a batch that are in the named states, in state order and then by serial number.
	 *
	 * State order is the order the states are declared in, PENDING first and EXCLUDED last, which is the order a reader
	 * opening the first page wants: what is still coming, then what went through, then what needs a human. Ordering by
	 * the column itself would sort the stored strings and answer alphabetically, which puts EXCLUDED first and is not
	 * what either this method or the response schema promises. A state added to the enum and not to the case sorts
	 * last.
	 *
	 * The states are filtered here rather than after the rows are read, or the page would be cut out of the whole batch
	 * and then filtered, and a page of ten would answer with however few of those ten happened to match.
	 *
	 * The caller passes every state when it wants every state. An empty collection answers with nothing, which is not
	 * what a caller who left the parameter out means by it.
	 *
	 * @param  batchId  the id of the batch
	 * @param  statuses the states to read, never empty
	 * @param  pageable which page to read
	 * @return          the page of computers
	 */
	@Query(value = "select computer from EndOfLeaseComputerEntity computer "
		+ "where computer.batch.id = :batchId "
		+ "and computer.status in :statuses "
		+ "order by case computer.status "
		+ "when se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING then 1 "
		+ "when se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.SENT then 2 "
		+ "when se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.FAILED then 3 "
		+ "when se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.EXCLUDED then 4 "
		+ "else 5 end, computer.serialNumber",
		countQuery = "select count(computer) from EndOfLeaseComputerEntity computer "
			+ "where computer.batch.id = :batchId "
			+ "and computer.status in :statuses")
	Page<EndOfLeaseComputerEntity> findByBatchId(@Param("batchId") String batchId, @Param("statuses") Collection<EndOfLeaseStatus> statuses, Pageable pageable);
}
