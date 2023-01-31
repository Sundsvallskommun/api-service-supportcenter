package apptest;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportcenter.Application;

@WireMockAppTestSuite(files = "classpath:/ReadConfiguration/", classes = Application.class)
class ReadConfigurationIT extends AbstractAppTest {

	private static final String POBKEY_HEADER_NAME = "pobKey";
	private static final String POBKEY_HEADER_VALUE = "xyz";
	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test001_readCaseCategories() {

		setupCall()
			.withServicePath("/configuration/caseCategories")
			.withHttpMethod(GET)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withExpectedResponse(RESPONSE_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test002_readClosureCodes() {

		setupCall()
			.withServicePath("/configuration/closureCodes")
			.withHttpMethod(GET)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withExpectedResponse(RESPONSE_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();
	}
}
