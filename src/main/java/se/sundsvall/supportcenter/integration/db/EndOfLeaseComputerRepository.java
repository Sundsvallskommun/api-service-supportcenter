package se.sundsvall.supportcenter.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus;

@CircuitBreaker(name = "endOfLeaseComputerRepository")
public interface EndOfLeaseComputerRepository extends JpaRepository<EndOfLeaseComputerEntity, String> {

	/**
	 * Counts the computers in a state. Used by the end of lease job to say how much is waiting for it.
	 *
	 * @param  status the state to count
	 * @return        the number of computers in the state
	 */
	long countByStatus(EndOfLeaseStatus status);
}
