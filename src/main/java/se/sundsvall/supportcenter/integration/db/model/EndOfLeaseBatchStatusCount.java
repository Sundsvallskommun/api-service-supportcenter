package se.sundsvall.supportcenter.integration.db.model;

import java.time.OffsetDateTime;

/**
 * How many computers of one batch are in one state, as one row of a grouped count.
 *
 * The columns of the batch itself ride along so that every batch a municipality has registered can be answered with one
 * query rather than one count per batch. A batch is registered with at least one computer, so no batch is missing from
 * a count taken from this side of the relation.
 *
 * @param batchId         the id of the batch
 * @param externalBatchId the id the sender gave the batch
 * @param created         when the batch was registered
 * @param status          the state the count is for
 * @param count           how many computers of the batch are in the state
 */
public record EndOfLeaseBatchStatusCount(String batchId, String externalBatchId, OffsetDateTime created, EndOfLeaseStatus status, long count) {
}
