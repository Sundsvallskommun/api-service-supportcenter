package se.sundsvall.supportcenter.api;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.supportcenter.api.model.Case;
import se.sundsvall.supportcenter.api.model.CreateCaseRequest;
import se.sundsvall.supportcenter.api.model.UpdateCaseRequest;
import se.sundsvall.supportcenter.service.CaseService;

@RestController
@Validated
@RequestMapping("/{municipalityId}/cases")
@Tag(name = "Cases", description = "Case operations")
class CaseResource {

	private final CaseService caseService;

	CaseResource(CaseService caseService) {
		this.caseService = caseService;
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE)
	@Operation(summary = "Create a new case", responses = {
		@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
			Problem.class, ConstraintViolationProblem.class
		}))),
		@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "409", description = "Conflict", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> createCase(
		@Parameter(name = "municipalityId", description = "Municipality Id", example = "2281") @PathVariable @ValidMunicipalityId String municipalityId,
		@Parameter(name = "pobKey", description = "The POB API-key", required = true) @RequestHeader("pobKey") final String pobKey,
		@RequestBody @Valid final CreateCaseRequest body) {

		final var caseId = caseService.createCase(pobKey, body);
		return created(fromPath("/{municipalityId}/cases/{caseId}").buildAndExpand(municipalityId, caseId).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@PatchMapping(path = "/{caseId}", consumes = APPLICATION_JSON_VALUE)
	@Operation(summary = "Update existing case", responses = {
		@ApiResponse(responseCode = "204", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
			Problem.class, ConstraintViolationProblem.class
		}))),
		@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> updateCase(
		@Parameter(name = "municipalityId", description = "Municipality Id", example = "2281") @PathVariable @ValidMunicipalityId String municipalityId,
		@Parameter(name = "pobKey", description = "The POB API-key", required = true) @RequestHeader("pobKey") String pobKey,
		@Parameter(name = "caseId", description = "The Case-ID", required = true, example = "81471222") @PathVariable(name = "caseId") String caseId,
		@RequestBody @Valid UpdateCaseRequest body) {

		caseService.updateCase(pobKey, caseId, body);
		return noContent().build();
	}

	@GetMapping(path = "/{caseId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Get a case by caseId", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
			Problem.class, ConstraintViolationProblem.class
		}))),
		@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Case> getCase(
		@Parameter(name = "municipalityId", description = "Municipality Id", example = "2281") @PathVariable @ValidMunicipalityId String municipalityId,
		@Parameter(name = "pobKey", description = "The POB API-key", required = true) @RequestHeader("pobKey") String pobKey,
		@Parameter(name = "caseId", description = "The Case-ID", required = true, example = "81471222") @PathVariable(name = "caseId") String caseId) {

		return ok(caseService.getCase(pobKey, caseId));
	}
}
