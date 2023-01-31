package se.sundsvall.supportcenter.service.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static se.sundsvall.dept44.test.annotation.resource.Load.ResourceType.JSON;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.jayway.jsonpath.PathNotFoundException;

import generated.client.pob.PobPayload;
import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;

@ExtendWith(ResourceLoaderExtension.class)
class CaseUtilTest {

	@ParameterizedTest
	@MethodSource("extractDataTestArguments")
	void extractData(String jsonPath, String expectedValue, @Load(value = "/CaseUtilTest/pobPayload.json", as = JSON) PobPayload pobPayload) {
		assertThat(CaseUtil.extractValueFromJsonPath(pobPayload, jsonPath, false)).isEqualTo(expectedValue);
	}

	@Test
	void extractDataWhenPathNotFoundAndExceptionsAreSuppressed(@Load(value = "/CaseUtilTest/pobPayload.json", as = JSON) PobPayload pobPayload) {
		assertThat(CaseUtil.extractValueFromJsonPath(pobPayload, "$['DoesntExist']", true)).isNull();
	}

	@Test
	void extractDataWhenPathNotFoundAndExceptionsAreNotSuppressed(@Load(value = "/CaseUtilTest/pobPayload.json", as = JSON) PobPayload pobPayload) {
		assertThrows(PathNotFoundException.class, () -> CaseUtil.extractValueFromJsonPath(pobPayload, "$['DoesntExist']", false));
	}

	@ParameterizedTest
	@MethodSource("existsTestArguments")
	void exists(String jsonPath, boolean expectedValue, @Load(value = "/CaseUtilTest/pobPayload.json", as = JSON) PobPayload pobPayload) {
		assertThat(CaseUtil.jsonPathExists(pobPayload, jsonPath)).isEqualTo(expectedValue);
	}

	private static Stream<Arguments> extractDataTestArguments() {
		return Stream.of(
			Arguments.of("$['Data']['Virtual.Shop_Kontaktperson']", "Test Testsson"),
			Arguments.of("$['Data']['OpenedBy']['Data']['FirstName']", "System API"),
			Arguments.of("$['Data']['CIInfo.Ci']['Data']['SerialNumber']", "9R5CNZl"),
			Arguments.of("$['Data']['CIInfo2.Ci']['Data']['SerialNumber']", "CQBRVY1"),
			Arguments.of("$['Data']['Id']", "867458"));
	}

	private static Stream<Arguments> existsTestArguments() {
		return Stream.of(
			Arguments.of("$['Data']['Virtual.Shop_Kontaktperson']", true),
			Arguments.of("$['Data']['OpenedBy']['Data']['FirstName']", true),
			Arguments.of("$['Data']['CIInfo.Ci']['Data']['SerialNumber']", true),
			Arguments.of("$['Data']['CIInfo2.Ci']['Data']['SerialNumber']", true),
			Arguments.of("$['Data']", true),
			Arguments.of("$", true),
			Arguments.of("$['DataApa']", false),
			Arguments.of("$['']", false),
			Arguments.of("$['Data']['CIInfo.Ci']['Data']['DoesntExist']", false),
			Arguments.of("$['Bla']['blabla']['blabla']['dummy']", false),
			Arguments.of("$['Data']['Dummy']", false));
	}
}
