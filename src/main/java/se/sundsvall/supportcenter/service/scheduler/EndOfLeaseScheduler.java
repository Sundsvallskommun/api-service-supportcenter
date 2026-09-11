package se.sundsvall.supportcenter.service.scheduler;

import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;

/**
 * Reports the computers that have reached end of lease to SysMan.
 *
 * One instance runs the job at a time, held off the others by the ShedLock row named by the configured name. The lock
 * is what keeps a computer from being reported once per instance, so lockAtMostFor has to stay longer than a run can
 * take. Run longer than it and the lock is handed to the next instance while this one is still working.
 */
@Component
class EndOfLeaseScheduler {

	private final EndOfLeaseSchedulerWorker endOfLeaseSchedulerWorker;

	EndOfLeaseScheduler(final EndOfLeaseSchedulerWorker endOfLeaseSchedulerWorker) {
		this.endOfLeaseSchedulerWorker = endOfLeaseSchedulerWorker;
	}

	@Dept44Scheduled(
		cron = "${scheduler.end-of-lease.cron}",
		name = "${scheduler.end-of-lease.name}",
		lockAtMostFor = "${scheduler.end-of-lease.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.end-of-lease.maximum-execution-time}")
	void reportPendingComputers() {
		endOfLeaseSchedulerWorker.processPendingComputers();
	}
}
