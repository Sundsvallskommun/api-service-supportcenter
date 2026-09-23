package se.sundsvall.supportcenter.service.processor;

import generated.client.pob.PobPayload;
import java.util.HashMap;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import se.sundsvall.supportcenter.api.model.Note;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.integration.pob.POBIntegration;
import se.sundsvall.supportcenter.service.ConfigurationService;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportcenter.api.model.enums.NoteType.SOLUTION;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.RESOLVED;
import static se.sundsvall.supportcenter.service.mapper.CommonMapper.toMemo;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CASE_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CI_INFO;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CI_INFO2;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_SHOP_CI_NAME;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.NOTE_HARDWARE_SWAP_PART;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.STATUS_SOLVED;
import static se.sundsvall.supportcenter.service.util.CaseUtil.extractValueFromJsonPath;
import static se.sundsvall.supportcenter.service.util.CaseUtil.jsonPathExists;

/**
 * Performs customizations on the PobPayload regarding serial number
 */
@Component
public class SerialNumberProcessor implements ProcessorInterface {

	private static final Logger LOG = LoggerFactory.getLogger(SerialNumberProcessor.class);
	private static final String JSON_PATH_EXISTING_SERIALNUMBER = "$['Data']['CIInfo.Ci']['Data']['SerialNumber']";
	private static final String JSON_PATH_EXISTING_HARDWARE_NAME = "$['Data']['CIInfo.Ci']['Data']['OptionalNumber']";
	private static final String NOTE_SEPARATOR = ". ";

	private final POBIntegration pobIntegration;
	private final ConfigurationService configurationService;

	public SerialNumberProcessor(POBIntegration pobIntegration, ConfigurationService configurationService) {
		this.pobIntegration = pobIntegration;
		this.configurationService = configurationService;
	}

	@Override
	public boolean shouldProcess(UpdateCaseRequest updateCaseRequest) {
		return nonNull(updateCaseRequest); // This processor should be executed for all requests as long as they are not null
	}

	/**
	 * Handle serial number customization on the PobPayload before POB update if serial number has been provided in the
	 * service request
	 */
	@Override
	public void preProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		ofNullable(updateCaseRequest.getSerialNumber()).filter(StringUtils::hasText).ifPresent(incomingSerialNumber -> {
			LOG.debug("SerialNumber provided in request, start processing 'preProcess'-logic: SerialNumber:'{}'", incomingSerialNumber);

			// Get serialNumber ID (from /configurationitems)
			final var serialNumberId = configurationService.getSerialNumberId(pobKey, incomingSerialNumber);

			// Get current case.
			final var fetchedCase = pobIntegration.getCase(pobKey, caseId);

			// Check if CIInfo.Ci exists and that it's serialNumber != the provided serialNumber.
			// If above is true: Set serialNumberId in CIInfo2.Ci (replaced hardware).
			if (jsonPathExists(fetchedCase, JSON_PATH_EXISTING_SERIALNUMBER)) {
				final var existingSerialNumber = extractValueFromJsonPath(fetchedCase, JSON_PATH_EXISTING_SERIALNUMBER, false);
				if (!existingSerialNumber.equalsIgnoreCase(incomingSerialNumber)) {
					LOG.debug("Adding serialNumberId:'{}' in '{}'", serialNumberId, KEY_CI_INFO2);
					pobPayload.getData().put(KEY_CI_INFO2, serialNumberId);
					addHardwareSwapNote(updateCaseRequest, pobPayload, fetchedCase);
					return;
				}
			}

			// If above is not true: Set serialNumberId in CIInfo.Ci (new hardware)
			LOG.debug("Adding serialNumberId:'{}' in '{}'", serialNumberId, KEY_CI_INFO);
			pobPayload.getData().put(KEY_CI_INFO, serialNumberId);
		});
	}

	/**
	 * Describes the hardware swap in the solution memo, provided that the case is being resolved and that both theft
	 * markings are known. The swap itself is decided by the caller, i.e. this is only called when the new configuration
	 * item has been set in CIInfo2.Ci.
	 */
	private void addHardwareSwapNote(UpdateCaseRequest updateCaseRequest, PobPayload pobPayload, PobPayload fetchedCase) {
		// An incoming 'Resolved' is mapped to two POB payloads, and only the 'Solved' one carries the solution memo.
		// Other statuses, e.g. 'Delivered', bring a solution memo of their own and are left alone.
		if (!Objects.equals(RESOLVED.getValue(), updateCaseRequest.getCaseStatus()) || !STATUS_SOLVED.equals(pobPayload.getData().get(KEY_CASE_STATUS))) {
			return;
		}

		final var replacedHardwareName = extractValueFromJsonPath(fetchedCase, JSON_PATH_EXISTING_HARDWARE_NAME, true);
		final var hardwareName = ofNullable(pobPayload.getData().get(KEY_SHOP_CI_NAME)).map(Object::toString).orElse(null);

		// Check that we have both the replaced hardwareName and the new hardwareName, otherwise we won't write anything.
		if (!StringUtils.hasText(replacedHardwareName) || !StringUtils.hasText(hardwareName)) {
			LOG.info("Skipping hardware swap note as replaced hardware name:'{}' or hardware name:'{}' is missing", replacedHardwareName, hardwareName);
			return;
		}

		LOG.info("Adding hardware swap note for replaced hardware:'{}' and new hardware:'{}'", replacedHardwareName, hardwareName);
		addToSolutionMemo(pobPayload, format(NOTE_HARDWARE_SWAP_PART, replacedHardwareName, hardwareName));
	}

	/**
	 * Appends the provided text to the solution memo sent by the supplier, or adds it as the solution memo when the
	 * supplier has not sent one.
	 */
	private void addToSolutionMemo(PobPayload pobPayload, String text) {
		final var memos = new HashMap<>(ofNullable(pobPayload.getMemo()).orElse(emptyMap()));

		ofNullable(memos.get(SOLUTION.toValue())).ifPresentOrElse(
			solutionMemo -> solutionMemo.memo(solutionMemo.getMemo() + NOTE_SEPARATOR + text),
			() -> memos.putAll(toMemo(Note.create().withType(SOLUTION).withText(text))));

		pobPayload.setMemo(memos);
	}

	@Override
	public void postProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		// Serial number processor has nothing to post process
	}
}
