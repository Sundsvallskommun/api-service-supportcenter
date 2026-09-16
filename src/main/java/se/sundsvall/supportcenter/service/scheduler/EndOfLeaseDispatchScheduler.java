package se.sundsvall.supportcenter.service.scheduler;

import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;

/**
 * Sends the message to the computers whose municipality is known, over the SysMan installation that municipality
 * belongs to.
 *
 * The second of the two end of lease runs. What it can pick up is what {@link EndOfLeaseLookupScheduler} has already
 * looked up, since the municipality is what decides the installation.
 *
 * One instance runs the job at a time, held off the others by the ShedLock row named by the configured name. The lock
 * is what keeps a computer from being sent its message once per instance, so lockAtMostFor has to stay longer than a
 * run can take. Run longer than it and the lock is handed to the next instance while this one is still working.
 */
@Component
class EndOfLeaseDispatchScheduler {

	private final EndOfLeaseDispatchWorker endOfLeaseDispatchWorker;

	EndOfLeaseDispatchScheduler(final EndOfLeaseDispatchWorker endOfLeaseDispatchWorker) {
		this.endOfLeaseDispatchWorker = endOfLeaseDispatchWorker;
	}

	@Dept44Scheduled(
		cron = "${scheduler.end-of-lease.dispatch.cron}",
		name = "${scheduler.end-of-lease.dispatch.name}",
		lockAtMostFor = "${scheduler.end-of-lease.dispatch.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.end-of-lease.dispatch.maximum-execution-time}")
	void dispatchComputers() {
		endOfLeaseDispatchWorker.processComputersReadyToSend();
	}
}
