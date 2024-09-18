package apptest;

import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportcenter.Application;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.CREATED;

@WireMockAppTestSuite(files = "classpath:/CreateAsset/", classes = Application.class)
class CreateAssetIT extends AbstractAppTest {

	private static final String POBKEY_HEADER_NAME = "pobKey";
	private static final String POBKEY_HEADER_VALUE = "xyz";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String PATH = "/2281/assets";

	@Test
	void test001_createAsset() {

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponse(RESPONSE_FILE)
			.withExpectedResponseStatus(CREATED)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test002_createAssetNoItemFound() {

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponse(RESPONSE_FILE)
			.withExpectedResponseStatus(CREATED)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test003_createAssetAuthenticationFailed() {

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponse(RESPONSE_FILE)
			.withExpectedResponseStatus(BAD_GATEWAY)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test004_createAssetSerialNumberAlreadyExists() {

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponse(RESPONSE_FILE)
			.withExpectedResponseStatus(BAD_GATEWAY)
			.sendRequestAndVerifyResponse();
	}
}
