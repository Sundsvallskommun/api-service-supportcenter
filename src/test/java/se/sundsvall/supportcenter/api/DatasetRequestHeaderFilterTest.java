package se.sundsvall.supportcenter.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetRequestHeaderFilterTest {

	private static final String DATASET_REQUEST_HEADER = "adv-dataset-request";
	private static final String DATASET_REQUEST = "DSET0001234";

	private final DatasetRequestHeaderFilter filter = new DatasetRequestHeaderFilter();

	@Test
	void returnsTheHeaderUnchanged() throws Exception {
		final var request = new MockHttpServletRequest("POST", "/2281/endOfLeaseBatches");
		request.addHeader(DATASET_REQUEST_HEADER, DATASET_REQUEST);
		final var response = new MockHttpServletResponse();
		final var chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getHeader(DATASET_REQUEST_HEADER)).isEqualTo(DATASET_REQUEST);
		assertThat(chain.getRequest()).isSameAs(request);
	}

	@Test
	void leavesTheHeaderOutWhenItIsMissing() throws Exception {
		final var request = new MockHttpServletRequest("POST", "/2281/endOfLeaseBatches");
		final var response = new MockHttpServletResponse();
		final var chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.containsHeader(DATASET_REQUEST_HEADER)).isFalse();
		assertThat(chain.getRequest()).isSameAs(request);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"", " "
	})
	void leavesTheHeaderOutWhenItIsEmpty(final String datasetRequest) throws Exception {
		final var request = new MockHttpServletRequest("POST", "/2281/endOfLeaseBatches");
		request.addHeader(DATASET_REQUEST_HEADER, datasetRequest);
		final var response = new MockHttpServletResponse();
		final var chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.containsHeader(DATASET_REQUEST_HEADER)).isFalse();
		assertThat(chain.getRequest()).isSameAs(request);
	}

	@ParameterizedTest
	@CsvSource({
		"GET, /2281/endOfLeaseBatches",
		"PUT, /2281/endOfLeaseBatches",
		"POST, /2281/endOfLeaseBatches/8f3c1e0a-2b4d-4f2e-9c7a-1d5e6f7a8b9c",
		"POST, /2281/endOfLeaseComputers/retry",
		"POST, /2281/cases",
		"POST, /endOfLeaseBatches"
	})
	void leavesOtherRequestsAlone(final String method, final String path) throws Exception {
		final var request = new MockHttpServletRequest(method, path);
		request.addHeader(DATASET_REQUEST_HEADER, DATASET_REQUEST);
		final var response = new MockHttpServletResponse();
		final var chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.containsHeader(DATASET_REQUEST_HEADER)).isFalse();
		assertThat(chain.getRequest()).isSameAs(request);
	}

	@Test
	void matchesThePathWithinTheApplication() throws Exception {
		final var request = new MockHttpServletRequest("POST", "/supportcenter/2281/endOfLeaseBatches");
		request.setContextPath("/supportcenter");
		request.addHeader(DATASET_REQUEST_HEADER, DATASET_REQUEST);
		final var response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getHeader(DATASET_REQUEST_HEADER)).isEqualTo(DATASET_REQUEST);
	}
}
