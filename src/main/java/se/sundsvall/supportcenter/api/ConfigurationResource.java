package se.sundsvall.supportcenter.api;

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
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.supportcenter.service.ConfigurationService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@Validated
@RequestMapping(path = "/{municipalityId}/configuration", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Configuration", description = "Configuration operations")
@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class ConfigurationResource {

	private final ConfigurationService configurationService;

	ConfigurationResource(final ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

	@GetMapping(path = "/caseCategories")
	@Operation(summary = "Return all available caseCategories", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<List<String>> getCaseCategoryList(
		@Parameter(name = "municipalityId", description = "Municipality Id", example = "2281") @PathVariable @ValidMunicipalityId final String municipalityId,
		@Parameter(name = "pobKey", description = "The POB API-key", required = true) @RequestHeader("pobKey") final String pobKey) {

		return ok(configurationService.getCaseCategoryList(pobKey));
	}

	@GetMapping(path = "/closureCodes")
	@Operation(summary = "Return all available closureCodes", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<List<String>> getClosureCodeList(
		@Parameter(name = "municipalityId", description = "Municipality Id", example = "2281") @PathVariable @ValidMunicipalityId final String municipalityId,
		@Parameter(name = "pobKey", description = "The POB API-key", required = true) @RequestHeader("pobKey") final String pobKey) {

		return ok(configurationService.getClosureCodeList(pobKey));
	}
}
