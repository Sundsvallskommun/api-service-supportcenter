package se.sundsvall.supportcenter.integration.db.model;

/**
 * The state a computer is in on its way to SysMan.
 */
public enum EndOfLeaseStatus {

	/**
	 * Waiting to be picked up, or waiting to be picked up again after an attempt that did not succeed.
	 */
	PENDING,

	/**
	 * Sent to SysMan. Terminal.
	 */
	SENT,

	/**
	 * The attempts are used up and the computer needs to be looked at by a human. Terminal until someone puts it back to
	 * PENDING.
	 */
	FAILED
}
