package apptest;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportcenter.Application;

@WireMockAppTestSuite(files = "classpath:/ReadAsset/", classes = Application.class)
class ReadAssetIT extends AbstractAppTest {

	private static final String POBKEY_HEADER_NAME = "pobKey";
	private static final String POBKEY_HEADER_VALUE = "xyz";
	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test001_readAsset() {

		setupCall()
			.withServicePath("/assets?serialNumber=FRGDZ1J")
			.withHttpMethod(GET)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withExpectedResponse(RESPONSE_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test002_readAssetNotFound() {

		setupCall()
			.withServicePath("/assets?serialNumber=XXX")
			.withHttpMethod(GET)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withExpectedResponse(RESPONSE_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test003_readAssetAuthenticationFailed() {

		setupCall()
			.withServicePath("/assets?serialNumber=YYY")
			.withHttpMethod(GET)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withExpectedResponse(RESPONSE_FILE)
			.withExpectedResponseStatus(BAD_GATEWAY)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test004_readAssetErrorInPOB() {

		setupCall()
			.withServicePath("/assets?serialNumber=YYY")
			.withHttpMethod(GET)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withExpectedResponse(RESPONSE_FILE)
			.withExpectedResponseStatus(BAD_GATEWAY)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test005_readAssetFailsAfterRetry() {

		setupCall()
			.withServicePath("/assets?serialNumber=YYY")
			.withHttpMethod(GET)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withExpectedResponse(RESPONSE_FILE)
			.withExpectedResponseStatus(INTERNAL_SERVER_ERROR)
			.sendRequestAndVerifyResponse();
	}
}
