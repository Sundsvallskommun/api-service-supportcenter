package se.sundsvall.supportcenter.service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseComputerRepository;

import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;

/**
 * The work the end of lease job does. Separate from the scheduler so that the scheduler stays the part that says when,
 * and this stays the part that says what.
 *
 * Reporting a computer to SysMan means reading its municipality from POB and then calling SysMan, and neither is
 * written yet. Until then this only says how much is waiting, so that the queue is visible in the logs before anything
 * drains it.
 */
@Component
public class EndOfLeaseSchedulerWorker {

	private static final Logger LOG = LoggerFactory.getLogger(EndOfLeaseSchedulerWorker.class);

	private final EndOfLeaseComputerRepository endOfLeaseComputerRepository;

	public EndOfLeaseSchedulerWorker(final EndOfLeaseComputerRepository endOfLeaseComputerRepository) {
		this.endOfLeaseComputerRepository = endOfLeaseComputerRepository;
	}

	public void processPendingComputers() {
		final var pendingComputers = endOfLeaseComputerRepository.countByStatus(PENDING);

		LOG.info("{} computer(s) are waiting to receive message via SysMan.", pendingComputers);
	}
}
