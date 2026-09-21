package se.sundsvall.supportcenter.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.supportcenter.api.model.EndOfLeaseStatisticsResponse;
import se.sundsvall.supportcenter.service.EndOfLeaseService;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@Validated
@RequestMapping(path = "/{municipalityId}/endOfLeaseStatistics", produces = APPLICATION_JSON_VALUE)
@Tag(name = "End of lease", description = "End of lease operations")
@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class EndOfLeaseStatisticsResource {

	private final EndOfLeaseService endOfLeaseService;

	EndOfLeaseStatisticsResource(final EndOfLeaseService endOfLeaseService) {
		this.endOfLeaseService = endOfLeaseService;
	}

	@GetMapping
	@Operation(summary = "How the end of lease batches of a municipality have gone",
		description = "Answers with what the batches of a window add up to and with the same counts one batch at a time, so that a batch worth looking closer at can be picked out and read by its id. The window is the month up to today when none is given, and the one that was answered is part of the answer. The counts are the state the computers are in now. A computer that failed and then went through counts as sent",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true)
		})
	ResponseEntity<EndOfLeaseStatisticsResponse> getEndOfLeaseStatistics(
		@Parameter(name = "municipalityId", description = "Municipality Id of the sender that registered the batches", example = "2281") @PathVariable @ValidMunicipalityId final String municipalityId,
		@Parameter(name = "from",
			description = "First day to count, by the day the batch was registered. A month back from the last day when left out",
			example = "2026-08-18") @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DATE) final LocalDate from,
		@Parameter(name = "to",
			description = "Last day to count, counted in full. Today when left out",
			example = "2026-09-18") @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DATE) final LocalDate to) {

		return ok(endOfLeaseService.getStatistics(municipalityId, from, to));
	}
}
