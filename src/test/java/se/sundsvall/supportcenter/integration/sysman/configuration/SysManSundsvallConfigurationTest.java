package se.sundsvall.supportcenter.integration.sysman.configuration;

import feign.okhttp.OkHttpClient;
import java.security.KeyStore;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import okhttp3.Protocol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder;
import se.sundsvall.dept44.security.Truststore;
import se.sundsvall.supportcenter.integration.sysman.configuration.SysManProperties.Instance;

import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static se.sundsvall.supportcenter.integration.sysman.configuration.SysManSundsvallConfiguration.CLIENT_ID;

@ExtendWith(MockitoExtension.class)
class SysManSundsvallConfigurationTest {

	@Spy
	private FeignMultiCustomizer feignMultiCustomizerSpy;

	@Mock
	private FeignBuilderCustomizer feignBuilderCustomizerMock;

	@Mock
	private Truststore truststoreMock;

	@Captor
	private ArgumentCaptor<JsonPathErrorDecoder> errorDecoderCaptor;

	@Test
	void testFeignBuilderCustomizer() {
		final var configuration = new SysManSundsvallConfiguration();
		final var connectTimeout = 123;
		final var readTimeout = 321;
		final var sysManProperties = new SysManProperties(connectTimeout, readTimeout, sundsvall(), ange());

		when(feignMultiCustomizerSpy.composeCustomizersToOne()).thenReturn(feignBuilderCustomizerMock);

		try (MockedStatic<FeignMultiCustomizer> feignMultiCustomizerMock = Mockito.mockStatic(FeignMultiCustomizer.class)) {
			feignMultiCustomizerMock.when(FeignMultiCustomizer::create).thenReturn(feignMultiCustomizerSpy);

			final var customizer = configuration.feignBuilderCustomizer(sysManProperties);

			feignMultiCustomizerMock.verify(FeignMultiCustomizer::create);

			verify(feignMultiCustomizerSpy).withErrorDecoder(errorDecoderCaptor.capture());
			verify(feignMultiCustomizerSpy).withRequestTimeoutsInSeconds(connectTimeout, readTimeout);
			verify(feignMultiCustomizerSpy).composeCustomizersToOne();

			// The bypass list is what keeps a rejected account distinguishable from a rejected call. Left off, every
			// 4xx arrives as BAD_GATEWAY and the branch that spares a municipality its attempts never runs.
			assertThat(errorDecoderCaptor.getValue())
				.isInstanceOf(JsonPathErrorDecoder.class)
				.hasFieldOrPropertyWithValue("bypassResponseCodes", List.of(UNAUTHORIZED.value(), FORBIDDEN.value()))
				.hasFieldOrPropertyWithValue("integrationName", CLIENT_ID);
			assertThat(customizer).isSameAs(feignBuilderCustomizerMock);
		}
	}

	@Test
	void testOkHttpClient() throws Exception {
		final var configuration = new SysManSundsvallConfiguration();
		final var trustManagerFactory = TrustManagerFactory.getInstance(getDefaultAlgorithm());
		trustManagerFactory.init((KeyStore) null);

		when(truststoreMock.getTrustManagerFactory()).thenReturn(trustManagerFactory);
		when(truststoreMock.getSSLContext()).thenReturn(SSLContext.getDefault());

		final var client = configuration.okHttpClient(new SysManProperties(1, 2, sundsvall(), ange()), truststoreMock);

		// The two slots carry different credentials on purpose. Asserting only that some authenticator is attached
		// would pass just as well with the other installation's account on it.
		assertThat(client).isInstanceOf(OkHttpClient.class);
		assertThat(client).extracting("delegate").extracting("authenticator")
			.isInstanceOf(NTLMAuthenticator.class)
			.hasFieldOrPropertyWithValue("domain", "PERSONAL")
			.hasFieldOrPropertyWithValue("username", "sundsvallUsername");

		// Asserted here only, since both installations are handed the same list by SysManFeignFactory. h2 left in the
		// list is what IIS resets the stream over, and no test that speaks plain HTTP would notice.
		assertThat(client).extracting("delegate").extracting("protocols")
			.isEqualTo(List.of(Protocol.HTTP_1_1));
	}

	private static Instance sundsvall() {
		return new Instance("2281", "http://sundsvall.url", "PERSONAL", "sundsvallUsername", "sundsvallPassword");
	}

	private static Instance ange() {
		return new Instance("2260", "http://ange.url", "ANGE", "angeUsername", "angePassword");
	}
}
