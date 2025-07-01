package se.sundsvall.supportcenter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import generated.client.pob.PobPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.supportcenter.integration.pob.POBIntegration;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

	@Mock
	private POBIntegration pobIntegrationMock;

	@InjectMocks
	private ConfigurationService configurationService;

	@Test
	void getClosureCodeList() {

		// Parameter values
		final var pobKey = "pobKey";

		// Mock
		when(pobIntegrationMock.getClosureCodes(pobKey)).thenReturn(setupListOfPobPayload("ClosureCode", "Id", "ClosureCode-1", "ClosureCode-2", "ClosureCode-3"));

		// Call
		final var result = configurationService.getClosureCodeList(pobKey);

		// Verification
		verify(pobIntegrationMock).getClosureCodes(pobKey);

		assertThat(result).isNotNull().hasSize(3).containsExactly("ClosureCode-1", "ClosureCode-2", "ClosureCode-3");
	}

	@Test
	void getCaseCategoryList() {

		// Parameter values
		final var pobKey = "pobKey";

		// Mock
		when(pobIntegrationMock.getCaseCategories(pobKey)).thenReturn(setupListOfPobPayload("CaseCategory", "Id", "CaseCategory-1", "CaseCategory-2", "CaseCategory-3"));

		// Call
		final var result = configurationService.getCaseCategoryList(pobKey);

		// Verification
		verify(pobIntegrationMock).getCaseCategories(pobKey);

		assertThat(result).isNotNull().hasSize(3).containsExactly("CaseCategory-1", "CaseCategory-2", "CaseCategory-3");
	}

	@Test
	void validateClosureCode() {

		// Parameter values
		final var pobKey = "pobKey";
		final var closureCode = "ClosureCode-2";

		// Mock
		when(pobIntegrationMock.getClosureCodes(pobKey)).thenReturn(setupListOfPobPayload("ClosureCode", "Id", "ClosureCode-1", "ClosureCode-2", "ClosureCode-3"));

		// Call
		configurationService.validateClosureCode(pobKey, closureCode);

		// Verification
		verify(pobIntegrationMock).getClosureCodes(pobKey);
	}

	@Test
	void validateClosureCodeWhenInvalid() {

		// Parameter values
		final var pobKey = "pobKey";
		final var closureCode = "invalidClosureCode";

		// Mock
		when(pobIntegrationMock.getClosureCodes(pobKey)).thenReturn(setupListOfPobPayload("ClosureCode", "Id", "ClosureCode-1", "ClosureCode-2", "ClosureCode-3"));

		// Call
		final var throwableProblem = assertThrows(ThrowableProblem.class, () -> configurationService.validateClosureCode(pobKey, closureCode));

		// Verification
		verify(pobIntegrationMock).getClosureCodes(pobKey);

		assertThat(throwableProblem).hasMessage("Bad Request: closureCode: 'invalidClosureCode' is not a valid value");
	}

	@Test
	void validateCaseCategory() {

		// Parameter values
		final var pobKey = "pobKey";
		final var caseCategory = "CaseCategory-2";

		// Mock
		when(pobIntegrationMock.getCaseCategories(pobKey)).thenReturn(setupListOfPobPayload("CaseCategory", "Id", "CaseCategory-1", "CaseCategory-2", "CaseCategory-3"));

		// Call
		configurationService.validateCaseCategory(pobKey, caseCategory);

		// Verification
		verify(pobIntegrationMock).getCaseCategories(pobKey);
	}

	@Test
	void validateCaseCategoryWhenInvalid() {

		// Parameter values
		final var pobKey = "pobKey";
		final var caseCategory = "invalidCaseCategory";

		// Mock
		when(pobIntegrationMock.getCaseCategories(pobKey)).thenReturn(setupListOfPobPayload("CaseCategory", "Id", "CaseCategory-1", "CaseCategory-2", "CaseCategory-3"));

		// Call
		final var throwableProblem = assertThrows(ThrowableProblem.class, () -> configurationService.validateCaseCategory(pobKey, caseCategory));

		// Verification
		verify(pobIntegrationMock).getCaseCategories(pobKey);

		assertThat(throwableProblem).hasMessage("Bad Request: caseCategory: 'invalidCaseCategory' is not a valid value");
	}

	@Test
	void getSerialNumberId() {

		// Parameter values
		final var pobKey = "pobKey";
		final var serialNumber = "serialNumber";
		final var id = "123456";

		// Mock
		when(pobIntegrationMock.getConfigurationItemsBySerialNumber(pobKey, serialNumber)).thenReturn(setupListOfPobPayload("ConfigurationItem", "Id", id));

		// Call
		final var result = configurationService.getSerialNumberId(pobKey, serialNumber);

		// Verification
		verify(pobIntegrationMock).getConfigurationItemsBySerialNumber(pobKey, serialNumber);

		assertThat(result).isNotNull().isEqualTo(id);
	}

	@Test
	void getConfigurationItemsBySerialNumberWhenEmptyIdIsReturned() {

		// Parameter values
		final var pobKey = "pobKey";
		final var serialNumber = "serialNumber";

		// Mock
		when(pobIntegrationMock.getConfigurationItemsBySerialNumber(pobKey, serialNumber)).thenReturn(setupListOfPobPayload("ConfigurationItem", "Id", " "));

		// Call
		final var throwableProblem = assertThrows(ThrowableProblem.class, () -> configurationService.getSerialNumberId(pobKey, serialNumber));

		// Verification
		verify(pobIntegrationMock).getConfigurationItemsBySerialNumber(pobKey, serialNumber);

		assertThat(throwableProblem).isNotNull().hasMessage("Bad Request: No ID for serialNumber: 'serialNumber' was found in POB-configurationitems");
	}

	private List<PobPayload> setupListOfPobPayload(String type, String key, String... values) {
		final var result = new ArrayList<PobPayload>();
		for (final String value : values) {
			result.add(new PobPayload()
				.type(type)
				.data(Map.of(key, value)));
		}
		return result;
	}
}
