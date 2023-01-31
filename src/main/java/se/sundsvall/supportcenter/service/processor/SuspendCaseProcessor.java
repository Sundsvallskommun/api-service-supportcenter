package se.sundsvall.supportcenter.service.processor;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.nonNull;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.AWAITING_INFO;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import generated.client.pob.PobPayload;
import generated.client.pob.SuspensionInfo;
import generated.client.pob.SuspensionInfo.TimeLimitsActionEnum;
import generated.client.pob.SuspensionInfo.UnitsEnum;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.integration.pob.POBClient;

/**
 * Performs action to suspend case
 */
@Component
public class SuspendCaseProcessor implements ProcessorInterface {
	private static final Logger LOG = LoggerFactory.getLogger(SuspendCaseProcessor.class);

	@Autowired
	private POBClient pobClient;

	@Override
	public boolean shouldProcess(UpdateCaseRequest updateCaseRequest) {
		return nonNull(updateCaseRequest) && Objects.equals(AWAITING_INFO.getValue(), updateCaseRequest.getCaseStatus());
	}

	/**
	 * Suspend case via the suspension API
	 */
	@Override
	public void preProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		LOG.debug("Start processing 'preProcess'-logic to suspend case with caseId:'{}'", caseId);
		pobClient.suspendCase(pobKey, caseId, generateSuspensionInfo());
	}

	@Override
	public void postProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		// Suspend case processor has nothing to post process
	}
	
	private SuspensionInfo generateSuspensionInfo() {
		return new SuspensionInfo()
			.suspension("Väntar kundsvar Advania")
			.timeLimitsAction(TimeLimitsActionEnum.NUMBER_2)
			.usePeriod(TRUE)
			.period(1095)
			.units(UnitsEnum.NUMBER_2);
	}
}
