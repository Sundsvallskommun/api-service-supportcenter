package se.sundsvall.supportcenter.service;

import static java.lang.String.format;
import static org.zalando.problem.Status.BAD_REQUEST;
import static se.sundsvall.supportcenter.service.mapper.ConfigurationMapper.toCaseCategoryList;
import static se.sundsvall.supportcenter.service.mapper.ConfigurationMapper.toClosureCodeList;
import static se.sundsvall.supportcenter.service.mapper.ConfigurationMapper.toConfigurationItemList;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.zalando.problem.Problem;
import se.sundsvall.supportcenter.integration.pob.POBIntegration;

@Service
public class ConfigurationService {

	private static final String VALIDATION_ERROR_TEMPLATE = "%s: '%s' is not a valid value";
	private static final String SERIAL_NUMBER_ERROR_TEMPLATE = "No ID for serialNumber: '%s' was found in POB-configurationitems";

	private final POBIntegration pobIntegration;

	public ConfigurationService(POBIntegration pobIntegration) {
		this.pobIntegration = pobIntegration;
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

	public void validateClosureCode(final String pobKey, final String closureCode) {
		// Ensure that provided closureCode exists in returned closureCode list from POB.
		if (getClosureCodeList(pobKey).stream().noneMatch(x -> x.equals(closureCode))) {
			throw Problem.valueOf(BAD_REQUEST, format(VALIDATION_ERROR_TEMPLATE, "closureCode", closureCode));
		}
	}

	public String getSerialNumberId(final String pobKey, final String serialNumber) {
		return toConfigurationItemList(pobIntegration.getConfigurationItemsBySerialNumber(pobKey, serialNumber)).stream()
			.filter(StringUtils::hasText)
			.findAny()
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, format(SERIAL_NUMBER_ERROR_TEMPLATE, serialNumber)));
	}
}
