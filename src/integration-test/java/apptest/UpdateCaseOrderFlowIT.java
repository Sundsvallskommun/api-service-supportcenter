package apptest;

import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportcenter.Application;

import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@WireMockAppTestSuite(files = "classpath:/UpdateCaseOrderFlow/", classes = Application.class)
class UpdateCaseOrderFlowIT extends AbstractAppTest {

	private static final String POBKEY_HEADER_NAME = "pobKey";
	private static final String POBKEY_HEADER_VALUE = "xyz";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String PATH = "/2281/cases";

	@Test
	void test001_assignBack() {

		setupCall()
			.withServicePath(PATH + "/867400")
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
			.withServicePath(PATH + "/867401")
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
			.withServicePath(PATH + "/867402")
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
			.withServicePath(PATH + "/867403")
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
			.withServicePath(PATH + "/867404")
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
			.withServicePath(PATH + "/867405")
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
			.withServicePath(PATH + "/867406")
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
			.withServicePath(PATH + "/867407")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test009_updateStatusToProcessed() {

		setupCall()
			.withServicePath(PATH + "/867409")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test010_updateStatusToReserved() {

		setupCall()
			.withServicePath(PATH + "/867410")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test011_updateStatusToPicking() {

		setupCall()
			.withServicePath(PATH + "/867411")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test012_updateStatusToDespatched() {

		setupCall()
			.withServicePath(PATH + "/867412")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test013_updateStatusToDelivered() {

		setupCall()
			.withServicePath(PATH + "/867413")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test014_updateStatusToDeliveredWithSerialNumber() {

		setupCall()
			.withServicePath(PATH + "/867414")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test015_updateStatusToDeliveredActionNeeded() {

		setupCall()
			.withServicePath(PATH + "/867415")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test016_updateStatusToOrderUpdated() {

		setupCall()
			.withServicePath(PATH + "/867415")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test017_updateStatusToScheduleChanged() {

		setupCall()
			.withServicePath(PATH + "/867415")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test018_updateStatusToEngineerStartWork() {

		setupCall()
			.withServicePath(PATH + "/867415")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test019_updateStatusToOrderNotCompleted() {

		setupCall()
			.withServicePath(PATH + "/867415")
			.withHttpMethod(PATCH)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}
}