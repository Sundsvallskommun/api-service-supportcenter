package se.sundsvall.supportcenter.integration.db.model;

/**
 * How many computers are in one state, as one row of a grouped count.
 *
 * Answers the whole of a window without naming the batches the computers came from. Counting is left to the database,
 * which hands back one row per state rather than the rows themselves, so a wide window costs a scan and not a heap
 * full of entities.
 *
 * @param status the state the count is for
 * @param count  how many computers are in the state
 */
public record EndOfLeaseStatusCount(EndOfLeaseStatus status, long count) {
}
