package se.sundsvall.supportcenter.service.processor;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CI_INFO;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CI_INFO2;
import static se.sundsvall.supportcenter.service.util.CaseUtil.extractValueFromJsonPath;
import static se.sundsvall.supportcenter.service.util.CaseUtil.jsonPathExists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import generated.client.pob.PobPayload;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.integration.pob.POBClient;
import se.sundsvall.supportcenter.service.ConfigurationService;
/**
 * Performs customizations on the PobPayload regarding serial number
 */
@Component
public class SerialNumberProcessor implements ProcessorInterface {
	private static final Logger LOG = LoggerFactory.getLogger(SerialNumberProcessor.class);
	private static final String JSON_PATH_EXISTING_SERIALNUMBER = "$['Data']['CIInfo.Ci']['Data']['SerialNumber']";

	@Autowired
	private POBClient pobClient;

	@Autowired
	private ConfigurationService configurationService;

	@Override
	public boolean shouldProcess(UpdateCaseRequest updateCaseRequest) {
		return nonNull(updateCaseRequest); //This processor should be executed for all requests as long as they are not null
	}

	/**
	 * Handle serial number customization on the PobPayload before POB update if serial number has been provided in the service request
	 */
	@Override
	public void preProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		ofNullable(updateCaseRequest.getSerialNumber()).filter(StringUtils::hasText).ifPresent(incomingSerialNumber -> {
			LOG.debug("SerialNumber provided in request, start processing 'preProcess'-logic: SerialNumber:'{}'", incomingSerialNumber);

			// Get serialNumber ID (from /configurationitems)
			final var serialNumberId = configurationService.getSerialNumberId(pobKey, incomingSerialNumber);

			// Get current case.
			final var fetchedCase = pobClient.getCase(pobKey, caseId);

			// Check if CIInfo.Ci exists and that it's serialNumber != the provided serialNumber.
			// If above is true: Set serialNumberId in CIInfo2.Ci (replaced hardware).
			if (jsonPathExists(fetchedCase, JSON_PATH_EXISTING_SERIALNUMBER)) {
				final var existingSerialNumber = extractValueFromJsonPath(fetchedCase, JSON_PATH_EXISTING_SERIALNUMBER, false);
				if (!existingSerialNumber.equalsIgnoreCase(incomingSerialNumber)) {
					LOG.debug("Adding serialNumberId:'{}' in '{}'", serialNumberId, KEY_CI_INFO2);
					pobPayload.getData().put(KEY_CI_INFO2, serialNumberId);
					return;
				}
			}

			// If above is not true: Set serialNumberId in CIInfo.Ci (new hardware)
			LOG.debug("Adding serialNumberId:'{}' in '{}'", serialNumberId, KEY_CI_INFO);
			pobPayload.getData().put(KEY_CI_INFO, serialNumberId);
		});
	}

	@Override
	public void postProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		// Serial number processor has nothing to post process
	}
}
