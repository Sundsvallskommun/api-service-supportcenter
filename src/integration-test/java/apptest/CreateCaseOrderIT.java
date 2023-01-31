package apptest;

import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportcenter.Application;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;

@WireMockAppTestSuite(files = "classpath:/CreateCaseOrder/", classes = Application.class)
class CreateCaseOrderIT extends AbstractAppTest {

	private static final String POBKEY_HEADER_NAME = "pobKey";
	private static final String POBKEY_HEADER_VALUE = "xyz";
	private static final String REQUEST_FILE = "request.json";

	@Test
	void test001_createCase() {

		setupCall()
			.withServicePath("/cases")
			.withHttpMethod(POST)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseBodyIsNull()
			.withExpectedResponseStatus(CREATED)
			.sendRequestAndVerifyResponse();
	}
}
