package se.sundsvall.supportcenter.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchEntity;

@CircuitBreaker(name = "endOfLeaseBatchRepository")
public interface EndOfLeaseBatchRepository extends JpaRepository<EndOfLeaseBatchEntity, String> {

	/**
	 * Redeclared rather than inherited so that the circuit breaker on this interface covers it. The breaker applies to
	 * the methods declared here, and a method inherited from JpaRepository is declared elsewhere.
	 *
	 * @param  <S>    the type of the batch to store
	 * @param  entity the batch to store
	 * @return        the stored batch
	 */
	@Override
	<S extends EndOfLeaseBatchEntity> S save(S entity);

	/**
	 * Finds the batch a sender stored under the id it gave the batch. Scoped by municipality because the id belongs to
	 * the sender's own numbering and says nothing about anyone else's.
	 *
	 * @param  municipalityId  the municipality of the sender
	 * @param  externalBatchId the id the sender gave the batch
	 * @return                 the batch, or empty when the sender has stored no batch under the id
	 */
	Optional<EndOfLeaseBatchEntity> findByMunicipalityIdAndExternalBatchId(String municipalityId, String externalBatchId);

	/**
	 * Counts the computers in a batch. A query rather than the size of the batch's own collection, which is lazy and
	 * would have to be loaded in full to be counted.
	 *
	 * @param  batchId the id of the batch
	 * @return         the number of computers in the batch
	 */
	@Query("select count(computer) from EndOfLeaseComputerEntity computer where computer.batch.id = :batchId")
	long countComputers(@Param("batchId") String batchId);
}
