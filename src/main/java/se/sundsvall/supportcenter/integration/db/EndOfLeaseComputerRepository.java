package se.sundsvall.supportcenter.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus;

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
	 * Held back rows are left out for the same reason as in {@link #findReadyToSend}.
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
}
