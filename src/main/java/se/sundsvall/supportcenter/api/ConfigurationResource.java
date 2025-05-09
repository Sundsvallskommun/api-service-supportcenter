package se.sundsvall.supportcenter.api;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.supportcenter.service.ConfigurationService;

@RestController
@Validated
@RequestMapping("/{municipalityId}/configuration")
@Tag(name = "Configuration", description = "Configuration operations")
class ConfigurationResource {

	private final ConfigurationService configurationService;

	ConfigurationResource(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

	@GetMapping(path = "/caseCategories", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Return all available caseCategories", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
			Problem.class, ConstraintViolationProblem.class
		}))),
		@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<List<String>> getCaseCategoryList(
		@Parameter(name = "municipalityId", description = "Municipality Id", example = "2281") @PathVariable @ValidMunicipalityId String municipalityId,
		@Parameter(name = "pobKey", description = "The POB API-key", required = true) @RequestHeader("pobKey") final String pobKey) {

		return ok(configurationService.getCaseCategoryList(pobKey));
	}

	@GetMapping(path = "/closureCodes", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Return all available closureCodes", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
			Problem.class, ConstraintViolationProblem.class
		}))),
		@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<List<String>> getClosureCodeList(
		@Parameter(name = "municipalityId", description = "Municipality Id", example = "2281") @PathVariable @ValidMunicipalityId String municipalityId,
		@Parameter(name = "pobKey", description = "The POB API-key", required = true) @RequestHeader("pobKey") final String pobKey) {

		return ok(configurationService.getClosureCodeList(pobKey));
	}
}
