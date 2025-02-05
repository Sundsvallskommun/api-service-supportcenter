package se.sundsvall.supportcenter.api;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
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
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.supportcenter.api.model.Asset;
import se.sundsvall.supportcenter.api.model.CreateAssetRequest;
import se.sundsvall.supportcenter.api.model.CreateAssetResponse;
import se.sundsvall.supportcenter.api.model.UpdateAssetRequest;
import se.sundsvall.supportcenter.service.AssetService;

@RestController
@Validated
@RequestMapping("/{municipalityId}/assets")
@Tag(name = "Asset", description = "Asset operations")
class AssetResource {

	private final AssetService assetService;

	AssetResource(AssetService assetService) {
		this.assetService = assetService;
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Create asset", responses = {
		@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
			Problem.class, ConstraintViolationProblem.class
		}))),
		@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<CreateAssetResponse> createAsset(
		@Parameter(name = "municipalityId", description = "Municipality Id", example = "2281") @PathVariable @ValidMunicipalityId String municipalityId,
		@Parameter(name = "pobKey", description = "The POB API-key", required = true) @RequestHeader("pobKey") final String pobKey,
		@RequestBody @Valid final CreateAssetRequest body) {

		final String pobId = assetService.createAsset(pobKey, body.withMunicipalityId(municipalityId));
		return created(fromPath("/{municipalityId}/assets").query("serialNumber={pobId}").build(municipalityId, pobId))
			.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
			.body(CreateAssetResponse.create().withId(pobId));
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Get assets", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
			Problem.class, ConstraintViolationProblem.class
		}))),
		@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<List<Asset>> getAssets(
		@Parameter(name = "municipalityId", description = "Municipality Id", example = "2281") @PathVariable @ValidMunicipalityId String municipalityId,
		@Parameter(name = "pobKey", description = "The POB API-key", required = true) @RequestHeader("pobKey") final String pobKey,
		@Parameter(name = "serialNumber", description = "The serial number of the asset", example = "4VV3RN2") @RequestParam(name = "serialNumber") final String serialNumber) {

		return ok(assetService.getConfigurationItemsBySerialNumber(pobKey, serialNumber));
	}

	@PatchMapping(path = "/{serialNumber}", consumes = APPLICATION_JSON_VALUE)
	@Operation(summary = "Update existing asset", responses = {
		@ApiResponse(responseCode = "204", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
			Problem.class, ConstraintViolationProblem.class
		}))),
		@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> updateAsset(
		@Parameter(name = "municipalityId", description = "Municipality Id", example = "2281") @PathVariable @ValidMunicipalityId String municipalityId,
		@Parameter(name = "pobKey", description = "The POB API-key", required = true) @RequestHeader("pobKey") final String pobKey,
		@Parameter(name = "serialNumber", description = "The serial number of the asset", required = true, example = "4VV3RN2") @PathVariable(name = "serialNumber") final String serialNumber,
		@RequestBody @Valid final UpdateAssetRequest body) {

		assetService.updateConfigurationItem(pobKey, serialNumber, body.withMunicipalityId(municipalityId));
		return noContent().build();
	}
}
