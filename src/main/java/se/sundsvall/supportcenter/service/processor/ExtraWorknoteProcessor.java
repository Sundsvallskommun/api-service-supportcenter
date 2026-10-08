package se.sundsvall.supportcenter.service.processor;

import generated.client.pob.PobPayload;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportcenter.api.model.Note;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;

import static java.util.Objects.nonNull;
import static se.sundsvall.supportcenter.api.model.enums.NoteType.WORKNOTE;
import static se.sundsvall.supportcenter.service.SupportCenterStatus.DELIVERED;
import static se.sundsvall.supportcenter.service.mapper.CommonMapper.toMemo;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.DELIVERED_STATUSES;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CASE_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.STATUS_SOLVED;

/**
 * Performs action to add memo to POB payload before sending it
 */
@Component
public class ExtraWorknoteProcessor implements ProcessorInterface {
	private static final Logger LOG = LoggerFactory.getLogger(ExtraWorknoteProcessor.class);

	@Override
	public boolean shouldProcess(UpdateCaseRequest updateCaseRequest) {
		// The null check is needed since DELIVERED_STATUSES is an immutable set, which throws on contains(null).
		return nonNull(updateCaseRequest) && nonNull(updateCaseRequest.getCaseStatus()) && DELIVERED_STATUSES.contains(updateCaseRequest.getCaseStatus());
	}

	/**
	 * Adding of extra memo object in the Solved PobPayload before update request is sent to POB
	 */
	@Override
	public void preProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		if (Objects.equals(STATUS_SOLVED, pobPayload.getData().get(KEY_CASE_STATUS))) {
			LOG.debug("Adding internal note to POB Solved payload for case with caseId:'{}'", caseId);
			pobPayload.getMemo().putAll(toMemo(Note.create().withType(WORKNOTE).withText(DELIVERED.getValue()), true));
		}
	}

	@Override
	public void postProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		// extra worknote processor has nothing to post process
	}
}
