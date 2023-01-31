package apptest;

import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import org.junit.jupiter.api.Test;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportcenter.Application;

@WireMockAppTestSuite(files = "classpath:/UpdateCaseOrderFlow/", classes = Application.class)
class UpdateCaseOrderFlowIT extends AbstractAppTest {

	private static final String POBKEY_HEADER_NAME = "pobKey";
	private static final String POBKEY_HEADER_VALUE = "xyz";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test001_assignBack() {

		setupCall()
			.withServicePath("/cases/867400")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseBodyIsNull()
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test002_createCaseReceipt() {

		setupCall()
			.withServicePath("/cases/867401")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseBodyIsNull()
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test003_resolveCase() {

		setupCall()
			.withServicePath("/cases/867402")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseBodyIsNull()
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test004_updateCase() {

		setupCall()
			.withServicePath("/cases/867403")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseBodyIsNull()
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test005_addCommentWorkNote() {

		setupCall()
			.withServicePath("/cases/867404")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseBodyIsNull()
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test006_updateSerialNumberReplaceHardware() {

		setupCall()
			.withServicePath("/cases/867405")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseBodyIsNull()
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test007_updateSerialNumberThatDoesntExistInConfigurationitems() {

		setupCall()
			.withServicePath("/cases/867406")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponse(RESPONSE_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test008_updateSerialNumberNewHardware() {

		setupCall()
			.withServicePath("/cases/867407")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test009_updateStatusToProcessed() {

		setupCall()
			.withServicePath("/cases/867409")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test010_updateStatusToReserved() {

		setupCall()
			.withServicePath("/cases/867410")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test011_updateStatusToPicking() {

		setupCall()
			.withServicePath("/cases/867411")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test012_updateStatusToDespatched() {

		setupCall()
			.withServicePath("/cases/867412")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test013_updateStatusToDelivered() {

		setupCall()
			.withServicePath("/cases/867413")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test014_updateStatusToDeliveredWithSerialNumber() {

		setupCall()
			.withServicePath("/cases/867414")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test015_updateStatusToDeliveredActionNeeded() {

		setupCall()
			.withServicePath("/cases/867415")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}
}