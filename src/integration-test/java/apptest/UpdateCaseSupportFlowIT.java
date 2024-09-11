package apptest;

import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import org.junit.jupiter.api.Test;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportcenter.Application;

@WireMockAppTestSuite(files = "classpath:/UpdateCaseSupportFlow/", classes = Application.class)
class UpdateCaseSupportFlowIT extends AbstractAppTest {

	private static final String POBKEY_HEADER_NAME = "pobKey";
	private static final String POBKEY_HEADER_VALUE = "xyz";
	private static final String REQUEST_FILE = "request.json";
	private static final String PATH = "/2281/cases";

	@Test
	void test001_openCase() {

		setupCall()
			.withServicePath(PATH + "/910277")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseBodyIsNull()
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test002_suspendCase() {

		setupCall()
			.withServicePath(PATH + "/910277")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseBodyIsNull()
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test003_unsuspendCase() {

		setupCall()
			.withServicePath(PATH + "/867591")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseBodyIsNull()
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test004_resolveCaseWithSerialNumber() {

		setupCall()
			.withServicePath(PATH + "/910277")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseBodyIsNull()
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test005_resolveCaseWithoutSerialNumber() {

		setupCall()
			.withServicePath(PATH + "/910277")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseBodyIsNull()
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test006_cancelCase() {

		setupCall()
			.withServicePath(PATH + "/867598")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseBodyIsNull()
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}
	
	@Test
	void test007_assignBackCase() {
		
		setupCall()
			.withServicePath(PATH + "/867597")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseBodyIsNull()
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}
}
