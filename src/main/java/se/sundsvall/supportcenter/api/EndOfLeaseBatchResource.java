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
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.EndOfLeaseBatchResponse;
import se.sundsvall.supportcenter.service.EndOfLeaseService;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
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
}
