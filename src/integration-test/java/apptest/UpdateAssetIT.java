package apptest;

import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import org.junit.jupiter.api.Test;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportcenter.Application;

@WireMockAppTestSuite(files = "classpath:/UpdateAsset/", classes = Application.class)
class UpdateAssetIT extends AbstractAppTest {

	private static final String POBKEY_HEADER_NAME = "pobKey";
	private static final String POBKEY_HEADER_VALUE = "xyz";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test001_updateAsset() {

		setupCall()
			.withServicePath("/assets/FRGDZ1J")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseBodyIsNull()
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test002_updateAssetAuthenticationFailed() {

		setupCall()
			.withServicePath("/assets/ERDOU7R")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponse(RESPONSE_FILE)
			.withExpectedResponseStatus(BAD_GATEWAY)
			.sendRequestAndVerifyResponse();
	}
}
