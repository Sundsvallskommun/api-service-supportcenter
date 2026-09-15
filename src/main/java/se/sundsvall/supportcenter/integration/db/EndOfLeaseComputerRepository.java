package se.sundsvall.supportcenter.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus;

@CircuitBreaker(name = "endOfLeaseComputerRepository")
public interface EndOfLeaseComputerRepository extends JpaRepository<EndOfLeaseComputerEntity, String> {

	/**
	 * The computers the lookup run has made ready to send, oldest first.
	 *
	 * @param  status   the state to look in
	 * @param  pageable how many to take
	 * @return          the computers whose municipality is known
	 */
	List<EndOfLeaseComputerEntity> findByStatusAndAssetMunicipalityIdIsNotNullOrderByCreated(EndOfLeaseStatus status, Pageable pageable);

	/**
	 * The computers the lookup run has left to do, oldest first.
	 *
	 * A null municipality is what says a computer has not been looked up yet, so no second state is needed to tell the
	 * two runs apart. Taken a page at a time rather than all at once, since every computer here is one POB call and a
	 * run has to finish inside its lock.
	 *
	 * @param  status   the state to look in
	 * @param  pageable how many to take
	 * @return          the computers waiting to be looked up
	 */
	List<EndOfLeaseComputerEntity> findByStatusAndAssetMunicipalityIdIsNullOrderByCreated(EndOfLeaseStatus status, Pageable pageable);

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
}
