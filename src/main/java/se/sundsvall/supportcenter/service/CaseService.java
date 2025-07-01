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

import generated.client.pob.PobPayload;
import generated.client.pob.PobPayloadWithTriggerResults;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.supportcenter.api.model.Case;
import se.sundsvall.supportcenter.api.model.CreateCaseRequest;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.integration.pob.POBIntegration;
import se.sundsvall.supportcenter.service.processor.ProcessorInterface;

@Service
public class CaseService {

	private final POBIntegration pobIntegration;
	private final ConfigurationService configurationService;
	private final List<ProcessorInterface> processors;

	public CaseService(POBIntegration pobIntegration, ConfigurationService configurationService, List<ProcessorInterface> processors) {
		this.pobIntegration = pobIntegration;
		this.configurationService = configurationService;
		this.processors = processors;
	}

	private static final String VALIDATION_ERROR_TEMPLATE = "%s: '%s' is not a valid value";

	public Case getCase(final String pobKey, final String caseId) {
		return toCase(pobIntegration.getCase(pobKey, caseId));
	}

	public void updateCase(final String pobKey, final String caseId, final UpdateCaseRequest updateCaseRequest) {
		// Will throw a runtime-exception if validation is unsuccessful.
		validate(pobKey, updateCaseRequest);

		// Convert request to PobPayload(s).
		toPobPayloads(caseId, updateCaseRequest).forEach(pobPayload -> {

			// Perform potential pre process actions.
			preProcess(pobKey, caseId, updateCaseRequest, pobPayload);

			// Call POB.
			if (shouldUpdatePOB(updateCaseRequest)) {
				pobIntegration.updateCase(pobKey, pobPayload);
			}

			// Perform potential post process actions.
			postProcess(pobKey, caseId, updateCaseRequest, pobPayload);
		});
	}

	public String createCase(final String pobKey, final CreateCaseRequest createCaseRequest) {
		// Will throw a runtime-exception if validation is unsuccessful.
		validate(pobKey, createCaseRequest);

		// Convert request to PobPayload and call Pob
		final var results = pobIntegration.createCase(pobKey, toPobPayload(createCaseRequest));

		return Optional.ofNullable(results).orElse(emptyList()).stream()
			.filter(Objects::nonNull)
			.findFirst()
			.map(PobPayloadWithTriggerResults::getData)
			.map(data -> data.get(KEY_ID))
			.map(Object::toString).orElse(null);
	}

	public List<String> getCaseCategoryList(final String pobKey) {
		return toCaseCategoryList(pobIntegration.getCaseCategories(pobKey));
	}

	public List<String> getClosureCodeList(final String pobKey) {
		return toClosureCodeList(pobIntegration.getClosureCodes(pobKey));
	}

	public void validateCaseCategory(final String pobKey, final String caseCategory) {
		// Ensure that provided caseCategory exists in returned caseCategory list from POB.
		if (getCaseCategoryList(pobKey).stream().noneMatch(x -> x.equals(caseCategory))) {
			throw Problem.valueOf(BAD_REQUEST, format(VALIDATION_ERROR_TEMPLATE, "caseCategory", caseCategory));
		}
	}

	private void validate(final String pobKey, final CreateCaseRequest updateCaseRequest) {
		Optional.ofNullable(updateCaseRequest.getCaseCategory()).ifPresent(value -> configurationService.validateCaseCategory(pobKey, value));
	}

	private void validate(final String pobKey, final UpdateCaseRequest updateCaseRequest) {
		Optional.ofNullable(updateCaseRequest.getCaseCategory()).ifPresent(value -> configurationService.validateCaseCategory(pobKey, value));
		Optional.ofNullable(updateCaseRequest.getClosureCode()).ifPresent(value -> configurationService.validateClosureCode(pobKey, value));
	}

	/**
	 * All incoming statuses should update POB except 'Awating Info'
	 *
	 * @param  updateCaseRequest incoming update request
	 * @return                   boolean if POB should be updated or not
	 */
	private boolean shouldUpdatePOB(final UpdateCaseRequest updateCaseRequest) {
		return !Objects.equals(AWAITING_INFO.getValue(), updateCaseRequest.getCaseStatus());
	}

	private void preProcess(final String pobKey, final String caseId, final UpdateCaseRequest updateCaseRequest, final PobPayload pobPayload) {
		processors.stream()
			.filter(processor -> processor.shouldProcess(updateCaseRequest))
			.forEach(processor -> processor.preProcess(pobKey, caseId, updateCaseRequest, pobPayload));
	}

	private void postProcess(final String pobKey, final String caseId, final UpdateCaseRequest updateCaseRequest, final PobPayload pobPayload) {
		processors.stream()
			.filter(processor -> processor.shouldProcess(updateCaseRequest))
			.forEach(processor -> processor.postProcess(pobKey, caseId, updateCaseRequest, pobPayload));
	}
}
