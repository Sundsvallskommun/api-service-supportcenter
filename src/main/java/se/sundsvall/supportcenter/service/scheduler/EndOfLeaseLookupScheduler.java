package se.sundsvall.supportcenter.service.scheduler;

import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;

/**
 * Reads the municipality of each waiting computer from POB and writes it on the computer's row.
 *
 * The first of the two end of lease runs. Nothing can be sent before this one has been over a computer, since the
 * municipality is what decides which SysMan installation the computer belongs to.
 *
 * One instance runs the job at a time, held off the others by the ShedLock row named by the configured name, so that a
 * computer is looked up once rather than once per instance. lockAtMostFor has to stay longer than a run can take.
 */
@Component
class EndOfLeaseLookupScheduler {

	private final EndOfLeaseLookupWorker endOfLeaseLookupWorker;

	EndOfLeaseLookupScheduler(final EndOfLeaseLookupWorker endOfLeaseLookupWorker) {
		this.endOfLeaseLookupWorker = endOfLeaseLookupWorker;
	}

	@Dept44Scheduled(
		cron = "${scheduler.end-of-lease.lookup.cron}",
		name = "${scheduler.end-of-lease.lookup.name}",
		lockAtMostFor = "${scheduler.end-of-lease.lookup.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.end-of-lease.lookup.maximum-execution-time}")
	void lookUpComputers() {
		endOfLeaseLookupWorker.processComputersAwaitingLookup();
	}
}
