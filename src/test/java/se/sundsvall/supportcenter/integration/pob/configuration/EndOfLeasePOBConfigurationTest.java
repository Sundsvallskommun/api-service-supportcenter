package se.sundsvall.supportcenter.integration.pob.configuration;

import feign.Request;
import feign.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.http.HttpStatus;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder;
import se.sundsvall.dept44.exception.ClientProblem;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static se.sundsvall.supportcenter.integration.pob.configuration.EndOfLeasePOBConfiguration.errorDecoder;
import static se.sundsvall.supportcenter.integration.pob.configuration.POBConfiguration.CLIENT_ID;

@ExtendWith(MockitoExtension.class)
class EndOfLeasePOBConfigurationTest {

	@Spy
	private FeignMultiCustomizer feignMultiCustomizerSpy;

	@Mock
	private FeignBuilderCustomizer feignBuilderCustomizerMock;

	@Mock
	private POBProperties propertiesMock;

	@Captor
	private ArgumentCaptor<JsonPathErrorDecoder> errorDecoderCaptor;

	@Test
	void testFeignBuilderCustomizer() {
		final var configuration = new EndOfLeasePOBConfiguration();
		final var connectTimeout = 123;
		final var readTimeout = 321;

		when(propertiesMock.connectTimeout()).thenReturn(connectTimeout);
		when(propertiesMock.readTimeout()).thenReturn(readTimeout);
		when(feignMultiCustomizerSpy.composeCustomizersToOne()).thenReturn(feignBuilderCustomizerMock);

		try (MockedStatic<FeignMultiCustomizer> feignMultiCustomizerMock = Mockito.mockStatic(FeignMultiCustomizer.class)) {
			feignMultiCustomizerMock.when(FeignMultiCustomizer::create).thenReturn(feignMultiCustomizerSpy);

			final var customizer = configuration.feignBuilderCustomizer(propertiesMock);

			feignMultiCustomizerMock.verify(FeignMultiCustomizer::create);

			verify(feignMultiCustomizerSpy).withErrorDecoder(errorDecoderCaptor.capture());
			verify(feignMultiCustomizerSpy).withRequestTimeoutsInSeconds(connectTimeout, readTimeout);
			verify(feignMultiCustomizerSpy).composeCustomizersToOne();

			// The two extra statuses over POBConfiguration are the whole reason this configuration exists. Reported
			// under the shared client id, since the failure is a failure talking to POB either way.
			assertThat(errorDecoderCaptor.getValue())
				.isInstanceOf(JsonPathErrorDecoder.class)
				.hasFieldOrPropertyWithValue("bypassResponseCodes", List.of(NOT_FOUND.value(), UNAUTHORIZED.value(), FORBIDDEN.value()))
				.hasFieldOrPropertyWithValue("integrationName", CLIENT_ID);
			assertThat(customizer).isSameAs(feignBuilderCustomizerMock);
		}
	}

	/**
	 * The branch in the lookup run that spares the queue its attempts turns on this status alone. Asserted against the
	 * decoder rather than against a ClientProblem written by hand, since a hand-written one passes whatever the decoder
	 * does, and what the decoder does is the thing that was wrong.
	 */
	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, names = {
		"UNAUTHORIZED", "FORBIDDEN"
	})
	void aRejectedKeyKeepsItsOwnStatus(final HttpStatus status) {
		assertThat(decode(status.value()))
			.isInstanceOf(ClientProblem.class)
			.extracting("status")
			.isEqualTo(status);
	}

	/**
	 * The other half of that branch. A 4xx about the row the call was made for has to keep costing that computer an
	 * attempt, so it must not arrive looking like a rejected key.
	 */
	@Test
	void everyOtherClientErrorIsStillReportedAsBadGateway() {
		assertThat(decode(BAD_REQUEST.value()))
			.isInstanceOf(ClientProblem.class)
			.extracting("status")
			.isEqualTo(BAD_GATEWAY);
	}

	private Exception decode(final int status) {
		final var request = Request.create(Request.HttpMethod.GET, "http://pob.url/configurationitems", Map.of(), null, null, null);
		final var response = Response.builder()
			.status(status)
			.reason(HttpStatus.valueOf(status).getReasonPhrase())
			.request(request)
			.body("""
				{"Message":"Authorization has been denied for this request."}""", UTF_8)
			.build();

		return errorDecoder().decode("EndOfLeasePOBClient#getConfigurationItemsBySerialNumber(String,String)", response);
	}
}
