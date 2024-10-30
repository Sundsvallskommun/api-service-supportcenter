package se.sundsvall.supportcenter.service.processor;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static se.sundsvall.supportcenter.api.model.enums.NoteType.SOLUTION;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.KEY_CASE_STATUS;
import static se.sundsvall.supportcenter.service.mapper.constant.CaseMapperConstants.STATUS_SOLVED;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import generated.client.pob.PobMemo;
import generated.client.pob.PobPayload;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;

/**
 * Performs customizations on the PobPayload regarding solution memo
 */
@Component
public class SolutionNoteProcessor implements ProcessorInterface {
	private static final Logger LOG = LoggerFactory.getLogger(SolutionNoteProcessor.class);

	@Override
	public boolean shouldProcess(UpdateCaseRequest updateCaseRequest) {
		return nonNull(updateCaseRequest); // This processor should be executed for all requests as long as they are not null
	}

	/**
	 * Removes solution memo from the PobPayload before POB update if status does not equal 'Solved' and solution memo is
	 * present
	 */
	@Override
	public void preProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		ofNullable(pobPayload)
			.filter(payload -> nonNull(payload.getData()))
			.filter(payload -> nonNull(payload.getMemo()))
			.filter(payload -> payload.getMemo().containsKey(SOLUTION.toValue()))
			.filter(payload -> !STATUS_SOLVED.equals(payload.getData().get(KEY_CASE_STATUS)))
			.ifPresent(payload -> {
				LOG.debug("CaseStatus is '{}' and solution memo is present, removing solution memo from payload", payload.getData().get(KEY_CASE_STATUS));
				final var filteredMemos = removeSolutionNote(payload);
				payload.setMemo(filteredMemos.isEmpty() ? null : filteredMemos);
			});
	}

	private Map<String, PobMemo> removeSolutionNote(PobPayload pobPayload) {
		return pobPayload.getMemo().entrySet().stream()
			.filter(entry -> !SOLUTION.toValue().equals(entry.getKey()))
			.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	@Override
	public void postProcess(String pobKey, String caseId, UpdateCaseRequest updateCaseRequest, PobPayload pobPayload) {
		// Serial number processor has nothing to post process
	}
}
