package se.sundsvall.supportcenter.service;

import org.springframework.stereotype.Service;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;

import static java.util.UUID.randomUUID;

/**
 * Placeholder until the persistence layer exists. Nothing is stored, nothing is sent, the caller gets an id back.
 */
@Service
public class EndOfLeaseService {

	public String registerBatch(final String municipalityId, final CreateEndOfLeaseBatchRequest createEndOfLeaseBatchRequest) {
		return randomUUID().toString();
	}
}
