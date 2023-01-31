package se.sundsvall.supportcenter.service.processor;

import static java.util.Objects.nonNull;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.OPEN;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_SUSPENSION;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

import generated.client.pob.PobPayload;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.integration.pob.POBClient;

/**
 * Performs action to remove suspension for case
 */
@Component
public class UnsuspendCaseProcessor implements ProcessorInterface {
	private static final Logger LOG = LoggerFactory.getLogger(UnsuspendCaseProcessor.class);

	@Autowired
	private POBClient pobClient;

	@Override
	public boolean shouldProcess(UpdateCaseRequest updateCaseRequest) {
		return nonNull(updateCaseRequest) && Objects.equals(OPEN.getValue(), updateCaseRequest.getCaseStatus());
	}

	@Override
	public void preProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		LOG.debug("Start processing 'preProcess'-logic to unsuspend case with caseId:'{}'", caseId);
		try {
			pobClient.getSuspension(pobKey, caseId); // If request returns answer (not 404), an ongoing suspension exists
			LOG.debug("Case '{}' is suspended, will delete suspension and modify payload data for key: '{}'", caseId, KEY_SUSPENSION);
			pobClient.deleteSuspension(pobKey, caseId); // Delete suspension
			pobPayload.getData().put(KEY_SUSPENSION, null); // Set suspension status to null on POB payload
		} catch (ThrowableProblem e) {
			// If POB returns other status than 404, re throw the exception
			if (e.getStatus() != Status.NOT_FOUND) {
				throw e;
			}
			LOG.debug("Case '{}' is not suspended, no further action is needed", caseId);
		}
	}

	@Override
	public void postProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		// Unsuspend case processor has nothing to post process
	}
}
