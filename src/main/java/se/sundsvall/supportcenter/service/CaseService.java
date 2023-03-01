package se.sundsvall.supportcenter.service;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.zalando.problem.Status.BAD_REQUEST;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.AWAITING_INFO;
import static se.sundsvall.supportcenter.service.mapper.CaseMapper.toCase;
import static se.sundsvall.supportcenter.service.mapper.CaseMapper.toPobPayload;
import static se.sundsvall.supportcenter.service.mapper.ConfigurationMapper.toCaseCategoryList;
import static se.sundsvall.supportcenter.service.mapper.ConfigurationMapper.toClosureCodeList;
import static se.sundsvall.supportcenter.service.mapper.UpdateCaseMapper.toPobPayloads;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_ID;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;

import generated.client.pob.PobPayload;
import generated.client.pob.PobPayloadWithTriggerResults;
import se.sundsvall.supportcenter.api.model.Case;
import se.sundsvall.supportcenter.api.model.CreateCaseRequest;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.integration.pob.POBClient;
import se.sundsvall.supportcenter.service.processor.ProcessorInterface;

@Service
public class CaseService {
	@Autowired
	private POBClient pobClient;

	@Autowired
	private ConfigurationService configurationService;

	@Autowired
	private List<ProcessorInterface> processors;

	private static final String VALIDATION_ERROR_TEMPLATE = "%s: '%s' is not a valid value";

	public Case getCase(String pobKey, String caseId) {
		return toCase(pobClient.getCase(pobKey, caseId));
	}

	public void updateCase(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest) {
		// Will throw a runtime-exception if validation is unsuccessful.
		validate(pobKey, updateCaseRequest);

		// Convert request to PobPayload(s).
		toPobPayloads(caseId, updateCaseRequest).forEach(pobPayload -> {

			// Perform potential pre process actions.
			preProcess(pobKey, caseId, updateCaseRequest, pobPayload);

			// Call POB.
			if (shouldUpdatePOB(updateCaseRequest)) {
				pobClient.updateCase(pobKey, pobPayload);
			}

			// Perform potential post process actions.
			postProcess(pobKey, caseId, updateCaseRequest, pobPayload);
		});
	}

	public String createCase(String pobKey, CreateCaseRequest createCaseRequest) {
		// Will throw a runtime-exception if validation is unsuccessful.
		validate(pobKey, createCaseRequest);

		// Convert request to PobPayload and call Pob
		var results = pobClient.createCase(pobKey, toPobPayload(createCaseRequest));

		return Optional.ofNullable(results).orElse(emptyList()).stream()
			.filter(Objects::nonNull)
			.findFirst()
			.map(PobPayloadWithTriggerResults::getData)
			.map(data -> data.get(KEY_ID))
			.map(Object::toString).orElse(null);
	}

	public List<String> getCaseCategoryList(String pobKey) {
		return toCaseCategoryList(pobClient.getCaseCategories(pobKey));
	}

	public List<String> getClosureCodeList(String pobKey) {
		return toClosureCodeList(pobClient.getClosureCodes(pobKey));
	}

	public void validateCaseCategory(String pobKey, String caseCategory) {
		// Ensure that provided caseCategory exists in returned caseCategory list from POB.
		if (getCaseCategoryList(pobKey).stream().noneMatch(x -> x.equals(caseCategory))) {
			throw Problem.valueOf(BAD_REQUEST, format(VALIDATION_ERROR_TEMPLATE, "caseCategory", caseCategory));
		}
	}

	private void validate(String pobKey, CreateCaseRequest updateCaseRequest) {
		Optional.ofNullable(updateCaseRequest.getCaseCategory()).ifPresent(value -> configurationService.validateCaseCategory(pobKey, value));
	}

	private void validate(String pobKey, UpdateCaseRequest updateCaseRequest) {
		Optional.ofNullable(updateCaseRequest.getCaseCategory()).ifPresent(value -> configurationService.validateCaseCategory(pobKey, value));
		Optional.ofNullable(updateCaseRequest.getClosureCode()).ifPresent(value -> configurationService.validateClosureCode(pobKey, value));
	}

	/**
	 * All incoming statuses should update POB except 'Awating Info'
	 * 
	 * @param updateCaseRequest incoming update request
	 * @return boolean if POB should be updated or not
	 */
	private boolean shouldUpdatePOB(UpdateCaseRequest updateCaseRequest) {
		return !Objects.equals(AWAITING_INFO.getValue(), updateCaseRequest.getCaseStatus());
	}

	private void preProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		processors.stream()
			.filter(processor -> processor.shouldProcess(updateCaseRequest))
			.forEach(processor -> processor.preProcess(pobKey, caseId, updateCaseRequest, pobPayload));
	}

	private void postProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		processors.stream()
			.filter(processor -> processor.shouldProcess(updateCaseRequest))
			.forEach(processor -> processor.postProcess(pobKey, caseId, updateCaseRequest, pobPayload));
	}
}
