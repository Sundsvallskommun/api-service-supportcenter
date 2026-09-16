package se.sundsvall.supportcenter.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.supportcenter.api.model.RetryEndOfLeaseComputersRequest;
import se.sundsvall.supportcenter.api.model.RetryEndOfLeaseComputersResponse;
import se.sundsvall.supportcenter.service.EndOfLeaseService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@Validated
@RequestMapping(path = "/{municipalityId}/endOfLeaseComputers", produces = APPLICATION_JSON_VALUE)
@Tag(name = "End of lease", description = "End of lease operations")
@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class EndOfLeaseComputerResource {

	private final EndOfLeaseService endOfLeaseService;

	EndOfLeaseComputerResource(final EndOfLeaseService endOfLeaseService) {
		this.endOfLeaseService = endOfLeaseService;
	}

	@PostMapping(path = "/retry", consumes = APPLICATION_JSON_VALUE)
	@Operation(summary = "Put computers that were given up on back in the queue",
		description = "A computer whose attempts are gone is left in a state the scheduled runs cannot take it out of. This is what puts it back, with its attempts reset. Answers with how many were taken, which is zero when there was nothing to take",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true)
		})
	ResponseEntity<RetryEndOfLeaseComputersResponse> retryEndOfLeaseComputers(
		@Parameter(name = "municipalityId", description = "Municipality Id of the sender that registered the batch", example = "2281") @PathVariable @ValidMunicipalityId final String municipalityId,
		@RequestBody @Valid final RetryEndOfLeaseComputersRequest body) {

		final var reset = endOfLeaseService.retryComputersGivenUpOn(municipalityId, body);
		return ok(RetryEndOfLeaseComputersResponse.create().withReset(reset));
	}
}
