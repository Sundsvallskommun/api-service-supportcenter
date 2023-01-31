package se.sundsvall.supportcenter.service.processor;

import static java.util.Objects.nonNull;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.ASSIGN_BACK;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CASE_STATUS;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import generated.client.pob.PobPayload;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;

/**
 * Performs action to alter (change/remove) caseStatus in POB payload before sending it
 * 
 * Right now 'AssignBack' is the only caseStatus that must be modified in POB payload as it has no counterpart in POB
 */
@Component
public class CaseStatusProcessor implements ProcessorInterface {
	private static final Logger LOG = LoggerFactory.getLogger(CaseStatusProcessor.class);

	@Override
	public boolean shouldProcess(UpdateCaseRequest updateCaseRequest) {
		return nonNull(updateCaseRequest) && Objects.equals(ASSIGN_BACK.getValue(), updateCaseRequest.getCaseStatus());
	}

	/**
	 * Customization of caseStatus in the PobPayload before update request is sent to POB
	 */
	@Override
	public void preProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		LOG.debug("Removing status '{}' from POB payload for case with caseId:'{}'", pobPayload.getData().get(KEY_CASE_STATUS), caseId);
		pobPayload.getData().remove(KEY_CASE_STATUS);
	}

	@Override
	public void postProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		// case status processor has nothing to post process
	}
}
