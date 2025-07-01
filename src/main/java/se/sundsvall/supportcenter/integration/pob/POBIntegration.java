package se.sundsvall.supportcenter.integration.pob;

import generated.client.pob.PobPayload;
import generated.client.pob.PobPayloadWithTriggerResults;
import generated.client.pob.SuspensionInfo;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class POBIntegration {

	private final POBClient pobClient;

	public POBIntegration(POBClient pobClient) {
		this.pobClient = pobClient;
	}

	/**
	 * Updates an existing case in POB.
	 *
	 * @param pobKey  the key to use for authorization
	 * @param payload the object with the updated case-attributes.
	 */
	public void updateCase(String pobKey, PobPayload payload) {
		pobClient.updateCase(pobKey, payload);
	}

	/**
	 * Creates a case in POB.
	 *
	 * @param  pobKey  the key to use for authorization
	 * @param  payload payload the object with the item to create.
	 * @return         a list of payload trigger results
	 */
	public List<PobPayloadWithTriggerResults> createCase(String pobKey, PobPayload payload) {
		return pobClient.createCase(pobKey, payload);
	}

	/**
	 * Get an existing case in POB.
	 *
	 * @param  pobKey the key to use for authorization
	 * @param  caseId the ID of the case.
	 * @return        The case
	 */
	public PobPayload getCase(String pobKey, String caseId) {
		return pobClient.getCase(pobKey, caseId);
	}

	/**
	 * Returns a list of all available case-categories.
	 *
	 * @param  pobKey the key to use for authorization
	 * @return        a list of available case-categories (max 10000)
	 */
	@Cacheable("case-categories")
	public List<PobPayload> getCaseCategories(String pobKey) {
		return pobClient.getCaseCategories(pobKey);
	}

	/**
	 * Returns a list of of all available closure-codes.
	 *
	 * @param  pobKey the key to use for authorization
	 * @return        a list of available closure-codes (max 10000)
	 */
	@Cacheable("closure-codes")
	public List<PobPayload> getClosureCodes(String pobKey) {
		return pobClient.getClosureCodes(pobKey);
	}

	/**
	 * Returns a list of configuration-items by serialNumber. The returned objects only contains the ID-attribute.
	 *
	 * @param  pobKey       the key to use for authorization
	 * @param  serialNumber the serial number to filter the results on
	 * @return              a list of configuration-items
	 */
	public List<PobPayload> getConfigurationItemsBySerialNumber(String pobKey, String serialNumber) {
		return pobClient.getConfigurationItemsBySerialNumber(pobKey, serialNumber);
	}

	/**
	 * Returns a list containing a item by id.
	 *
	 * @param  pobKey the key to use for authorization
	 * @param  id     id of an item
	 * @return        a list containing an item
	 */
	public List<PobPayload> getItemsById(String pobKey, String id) {
		return pobClient.getItemsById(pobKey, id);
	}

	/**
	 * Returns a list of items by model-name. The returned objects only contains the ID-attribute.
	 *
	 * @param  pobKey    the key to use for authorization
	 * @param  modelName the item model name to filter response on
	 * @return           a list of configuration-items
	 */
	public List<PobPayload> getItemsByModelName(String pobKey, String modelName) {
		return pobClient.getItemsByModelName(pobKey, modelName);
	}

	/**
	 * Creates an item
	 *
	 * @param  pobKey  the key to use for authorization
	 * @param  payload the object with the updated configuration attributes
	 * @return         list of pob-payloads with triggered results
	 */
	public List<PobPayloadWithTriggerResults> createItem(String pobKey, PobPayload payload) {
		return pobClient.createItem(pobKey, payload);
	}

	/**
	 * Updates a configuration-item
	 *
	 * @param  pobKey  the key to use for authorization
	 * @param  payload the object with configuration-items to create
	 * @return         list of pob-payloads with triggered results
	 */
	public List<PobPayloadWithTriggerResults> createConfigurationItem(String pobKey, PobPayload payload) {
		return pobClient.createConfigurationItem(pobKey, payload);
	}

	/**
	 * Updates a configuration-item
	 *
	 * @param pobKey  the key to use for authorization
	 * @param payload the object with the updated configuration attributes
	 */
	public void updateConfigurationItem(String pobKey, PobPayload payload) {
		pobClient.updateConfigurationItem(pobKey, payload);
	}

	/**
	 * Suspend an existing case in POB.
	 *
	 * @param pobKey  the key to use for authorization
	 * @param caseId  the ID of the case
	 * @param payload object with suspension information
	 */
	public void suspendCase(String pobKey, String caseId, SuspensionInfo payload) {
		pobClient.suspendCase(pobKey, caseId, payload);
	}

	/**
	 * Get suspension information for a suspended case in POB, if case is not suspended a 404 will be thrown.
	 *
	 * @param  pobKey the key to use for authorization
	 * @param  caseId the ID of the case
	 * @return        Information about the suspension
	 */
	public SuspensionInfo getSuspension(String pobKey, String caseId) {
		return pobClient.getSuspension(pobKey, caseId);
	}

	/**
	 * Unsuspend a suspended case in POB.
	 *
	 * @param pobKey the key to use for authorization
	 * @param caseId the ID of the case
	 */
	public void deleteSuspension(String pobKey, String caseId) {
		pobClient.deleteSuspension(pobKey, caseId);
	}

}
