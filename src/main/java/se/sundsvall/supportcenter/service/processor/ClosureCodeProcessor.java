package se.sundsvall.supportcenter.service.processor;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.RESOLVED;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.CLOSURE_CODE_CHANGE_OF_HARDWARE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CASE_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CLOSURE_CODE;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.STATUS_SOLVED;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import generated.client.pob.PobPayload;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;

/**
 * Performs action to modify closure code depending on incoming request content
 */
@Component
public class ClosureCodeProcessor implements ProcessorInterface {
	private static final Logger LOG = LoggerFactory.getLogger(ClosureCodeProcessor.class);

	@Override
	public boolean shouldProcess(UpdateCaseRequest updateCaseRequest) {
		return nonNull(updateCaseRequest) && Objects.equals(RESOLVED.getValue(), updateCaseRequest.getCaseStatus());
	}

	/**
	 * Handle closure code customization on the PobPayload for status 'Solved' before POB update if serial number has 
	 * been provided in the service request
	 */
	@Override
	public void preProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		ofNullable(updateCaseRequest.getSerialNumber()).filter(StringUtils::hasText).ifPresent(incomingSerialNumber -> {
			LOG.debug("SerialNumber provided in request, start processing 'preProcess'-logic: SerialNumber:'{}'", incomingSerialNumber);
			
			if (Objects.equals(STATUS_SOLVED, pobPayload.getData().get(KEY_CASE_STATUS))) {
				LOG.debug("POB payload has status '{}', proceeding with modification of closureCode to '{}'", STATUS_SOLVED, CLOSURE_CODE_CHANGE_OF_HARDWARE);
				pobPayload.getData().put(KEY_CLOSURE_CODE, CLOSURE_CODE_CHANGE_OF_HARDWARE);
			}
		});
	}

	@Override
	public void postProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		// closure code processor has nothing to post process
	}
}
