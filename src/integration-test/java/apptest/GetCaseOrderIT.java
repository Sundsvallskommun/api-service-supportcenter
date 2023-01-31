package apptest;

import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportcenter.Application;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

@WireMockAppTestSuite(files = "classpath:/GetCaseOrder/", classes = Application.class)
class GetCaseOrderIT extends AbstractAppTest {

	private static final String POBKEY_HEADER_NAME = "pobKey";
	private static final String POBKEY_HEADER_VALUE = "xyz";
	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test001_getCase() {

		setupCall()
			.withServicePath("/cases/111222")
			.withHttpMethod(GET)
			.withHeader(POBKEY_HEADER_NAME, POBKEY_HEADER_VALUE)
			.withExpectedResponse(RESPONSE_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();
	}
}
