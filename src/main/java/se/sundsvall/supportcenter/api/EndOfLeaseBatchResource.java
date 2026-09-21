package se.sundsvall.supportcenter.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.dept44.common.validators.annotation.OneOf;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.EndOfLeaseBatchResponse;
import se.sundsvall.supportcenter.api.model.EndOfLeaseBatchStatusResponse;
import se.sundsvall.supportcenter.service.EndOfLeaseService;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;
import static se.sundsvall.supportcenter.api.DatasetRequestHeaderFilter.DATASET_REQUEST_HEADER;

/**
 * The adv-dataset-request header is returned by {@link DatasetRequestHeaderFilter}, not here, so that it is on the
 * error responses too.
 */
@RestController
@Validated
@RequestMapping(path = "/{municipalityId}/endOfLeaseBatches", produces = APPLICATION_JSON_VALUE)
@Tag(name = "End of lease", description = "End of lease operations")
@ApiResponse(responseCode = "400",
	description = "Bad Request",
	headers = @Header(name = DATASET_REQUEST_HEADER, description = EndOfLeaseBatchResource.DATASET_REQUEST_HEADER_DESCRIPTION, schema = @Schema(type = "string")),
	content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	})))
@ApiResponse(responseCode = "500",
	description = "Internal Server Error",
	headers = @Header(name = DATASET_REQUEST_HEADER, description = EndOfLeaseBatchResource.DATASET_REQUEST_HEADER_DESCRIPTION, schema = @Schema(type = "string")),
	content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class EndOfLeaseBatchResource {

	static final String DATASET_REQUEST_HEADER_DESCRIPTION = "The adv-dataset-request header of the request, unchanged. Left out when that header is missing or empty";

	private final EndOfLeaseService endOfLeaseService;

	EndOfLeaseBatchResource(final EndOfLeaseService endOfLeaseService) {
		this.endOfLeaseService = endOfLeaseService;
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE)
	@Operation(summary = "Register a batch of computers that have reached end of lease", responses = {
		@ApiResponse(responseCode = "202", headers = {
			@Header(name = LOCATION, schema = @Schema(type = "string")),
			@Header(name = DATASET_REQUEST_HEADER, description = DATASET_REQUEST_HEADER_DESCRIPTION, schema = @Schema(type = "string"))
		}, description = "Batch accepted for processing", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "409",
			description = "Conflict, a batch with the same external id is already registered. The detail names the stored batch",
			headers = @Header(name = DATASET_REQUEST_HEADER, description = DATASET_REQUEST_HEADER_DESCRIPTION, schema = @Schema(type = "string")),
			content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	@Parameter(in = ParameterIn.HEADER, name = DATASET_REQUEST_HEADER, description = "Set by the sender. Returned unchanged on every response", example = "DSET0001234", schema = @Schema(type = "string"))
	ResponseEntity<EndOfLeaseBatchResponse> createEndOfLeaseBatch(
		@Parameter(name = "municipalityId", description = "Municipality Id of the sender. The municipality of each computer is resolved from POB and may differ", example = "2281") @PathVariable @ValidMunicipalityId final String municipalityId,
		@RequestBody @Valid final CreateEndOfLeaseBatchRequest body) {

		final var batchId = endOfLeaseService.registerBatch(municipalityId, body);
		return accepted()
			.location(fromPath("/{municipalityId}/endOfLeaseBatches/{batchId}").buildAndExpand(municipalityId, batchId).toUri())
			.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
			.body(EndOfLeaseBatchResponse.create().withId(batchId).withExternalBatchId(body.getExternalBatchId()));
	}

	@GetMapping(path = "/{batchId}")
	@Operation(summary = "Read one batch with the computers in it",
		description = "Answers with the state each computer of the batch is in and, for the ones that were given up on, why. The reason is what the last attempt said, and a run that gets through clears it, so a computer that is SENT carries none even when it took several tries. The counts cover the whole batch whichever states are asked for",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true)
		})
	@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	ResponseEntity<EndOfLeaseBatchStatusResponse> getEndOfLeaseBatch(
		@Parameter(name = "municipalityId", description = "Municipality Id of the sender that registered the batch", example = "2281") @PathVariable @ValidMunicipalityId final String municipalityId,
		@Parameter(name = "batchId", description = "Id of the batch, as answered by the call that registered it", example = "8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c") @PathVariable @ValidUuid final String batchId,
		@Parameter(name = "status",
			description = "States to list, one or more of PENDING, SENT, FAILED and EXCLUDED. Every state is listed when the parameter is left out",
			example = "FAILED") @RequestParam(name = "status", required = false) final List<@OneOf({
				"PENDING", "SENT", "FAILED", "EXCLUDED"
		}) String> statuses) {

		return ok(endOfLeaseService.getBatchStatus(municipalityId, batchId, statuses));
	}
}
