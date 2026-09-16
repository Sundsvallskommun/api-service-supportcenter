package se.sundsvall.supportcenter.integration.db;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportcenter.Application;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseBatchEntity;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;

/**
 * What the circuit breaker on the repository interface actually covers.
 *
 * The annotation reaches the methods declared on the interface and stops there, so a method inherited from
 * JpaRepository is outside it however the interface is annotated. That is three framework internals deep and would
 * break silently on a Spring Cloud or resilience4j upgrade, taking every write the two runs make out of the breaker
 * while leaving every read inside it. Pinned here rather than believed.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
@Transactional
class EndOfLeaseComputerRepositoryCircuitBreakerTest {

	@Autowired
	private EndOfLeaseComputerRepository endOfLeaseComputerRepository;

	@Autowired
	private EndOfLeaseBatchRepository endOfLeaseBatchRepository;

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@Test
	void breakerRecordsTheWritesTheRunsMake() {
		final var breaker = circuitBreakerRegistry.circuitBreaker("endOfLeaseComputerRepository");
		breaker.reset();

		final var computer = aComputer();

		endOfLeaseComputerRepository.save(computer);
		assertThat(breaker.getMetrics().getNumberOfBufferedCalls())
			.as("save is redeclared on the interface, so the breaker records it")
			.isEqualTo(1);

		endOfLeaseComputerRepository.saveAll(List.of(computer));
		assertThat(breaker.getMetrics().getNumberOfBufferedCalls())
			.as("saveAll is redeclared on the interface, so the breaker records it")
			.isEqualTo(2);

		endOfLeaseComputerRepository.countByStatus(PENDING);
		assertThat(breaker.getMetrics().getNumberOfBufferedCalls())
			.as("a derived query declared on the interface is recorded too")
			.isEqualTo(3);
	}

	/**
	 * The other half of the same fact. flush() is inherited and deliberately not redeclared, since nothing here calls
	 * it, and it goes unrecorded. When this starts failing the framework has begun covering inherited methods by
	 * itself, and the redeclarations above are free to go.
	 */
	@Test
	void breakerDoesNotReachAnInheritedMethod() {
		final var breaker = circuitBreakerRegistry.circuitBreaker("endOfLeaseComputerRepository");
		breaker.reset();

		endOfLeaseComputerRepository.flush();

		assertThat(breaker.getMetrics().getNumberOfBufferedCalls()).isZero();
	}

	private EndOfLeaseComputerEntity aComputer() {
		final var batch = endOfLeaseBatchRepository.save(EndOfLeaseBatchEntity.create()
			.withMunicipalityId("2281")
			.withExternalBatchId(randomUUID().toString()));

		return EndOfLeaseComputerEntity.create()
			.withBatch(batch)
			.withSerialNumber("J123ABC")
			.withAssetTag("AB12345")
			.withEndOfLeaseDate(LocalDate.of(2026, 12, 31))
			.withStatus(PENDING);
	}
}
