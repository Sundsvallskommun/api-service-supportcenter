package se.sundsvall.supportcenter.service.processor;

import generated.client.pob.PobPayload;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;

public interface ProcessorInterface {

	/**
	 * Method to determine if pre and post processing should be executed
	 * @param updateCaseRequest the incoming request, used for determining if processor should be engaged or not
	 * @return true if processor should execute pre and post processing methods
	 */
	boolean shouldProcess(UpdateCaseRequest updateCaseRequest);
	
	/**
	 * Logic to execute before update request is sent to POB
	 * @param pobKey client key to POB
	 * @param caseId id for the case in pob
	 * @param updateCaseRequest incoming supportcenter request
	 * @param pobPayload POB payload
	 */
	void preProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload);

	/**
	 * Logic to execute after update request has been sent to POB
	 * @param pobKey client key to POB
	 * @param caseId id for the case in pob
	 * @param updateCaseRequest incoming supportcenter request
	 * @param pobPayload POB payload
	 */
	void postProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload);
}
