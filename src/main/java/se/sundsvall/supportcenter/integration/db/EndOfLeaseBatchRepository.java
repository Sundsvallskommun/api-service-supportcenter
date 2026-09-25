package se.sundsvall.supportcenter.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	/**
	 * Finds one batch of a municipality. Scoped by municipality so that holding an id is not on its own enough to read
	 * another sender's batch.
	 *
	 * @param  id             the id of the batch
	 * @param  municipalityId the municipality of the sender that registered it
	 * @return                the batch, or empty when the municipality has no batch under the id
	 */
	Optional<EndOfLeaseBatchEntity> findByIdAndMunicipalityId(String id, String municipalityId);

	/**
	 * One page of the batches a municipality registered inside a window, newest first.
	 * The batches are paged here rather than the counts being paged, because the counts are grouped by batch and state
	 * and a page cut out of those would end in the middle of a batch. Paging the batch table instead means one page is
	 * a whole number of batches, and the number of batches in the window comes back as the total without a query of its
	 * own.
	 *
	 * @param  municipalityId the municipality of the sender that registered the batches
	 * @param  from           the first moment to read, included
	 * @param  to             the moment to stop reading at, not included
	 * @param  pageable       which page to read
	 * @return                the page of batches
	 */
	@Query(value = "select batch from EndOfLeaseBatchEntity batch "
		+ "where batch.municipalityId = :municipalityId "
		+ "and batch.created >= :from "
		+ "and batch.created < :to "
		+ "order by batch.created desc, batch.id",
		countQuery = "select count(batch) from EndOfLeaseBatchEntity batch "
			+ "where batch.municipalityId = :municipalityId "
			+ "and batch.created >= :from "
			+ "and batch.created < :to")
	Page<EndOfLeaseBatchEntity> findInWindow(@Param("municipalityId") String municipalityId, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to, Pageable pageable);
}
