package se.sundsvall.supportcenter.integration.sysman.configuration;

import feign.Client;
import feign.okhttp.OkHttpClient;
import java.util.List;
import javax.net.ssl.X509TrustManager;
import okhttp3.Protocol;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder.JsonPathSetup;
import se.sundsvall.dept44.security.Truststore;
import se.sundsvall.supportcenter.integration.sysman.configuration.SysManProperties.Instance;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * The Feign pieces the two SysMan installations have in common. They differ in their host and their account and in
 * nothing else, so the parts that are the same are built here rather than copied into both configurations.
 */
final class SysManFeignFactory {

	static final String TITLE_PATH = "$.message";

	static final String DETAIL_PATH = "$..details.concat()";

	private SysManFeignFactory() {}

	/**
	 * The client FeignConfiguration contributes, with the NTLM authenticator added. The truststore half is kept as it
	 * is there: dept44 installs its certificates as the only trust anchors of the process, so a client built without
	 * them trusts nothing we talk to.
	 *
	 * The protocol list is pinned because NTLM is bound to one connection and IIS will not run Windows Authentication
	 * over a multiplexed one. Left to itself OkHttp offers h2 over ALPN, IIS answers the first request by resetting the
	 * stream with HTTP_1_1_REQUIRED, and OkHttp does not retry that on HTTP/1.1 of its own accord, so the handshake
	 * never starts. Nothing under test catches it: WireMock and MockWebServer both serve plain HTTP, which is HTTP/1.1
	 * regardless of what we ask for.
	 *
	 * @param  instance   the installation whose account answers the challenge
	 * @param  truststore the certificates the process trusts
	 * @return            the client
	 */
	static Client okHttpClient(final Instance instance, final Truststore truststore) {
		final var trustManager = (X509TrustManager) truststore.getTrustManagerFactory().getTrustManagers()[0];

		return new OkHttpClient(new okhttp3.OkHttpClient.Builder()
			.protocols(List.of(Protocol.HTTP_1_1))
			.sslSocketFactory(truststore.getSSLContext().getSocketFactory(), trustManager)
			.authenticator(new NTLMAuthenticator(instance.domain(), instance.username(), instance.password()))
			.build());
	}

	/**
	 * @param  clientId         the name the failure is reported under
	 * @param  sysManProperties the timeouts, which both installations share
	 * @return                  the composed customizer
	 */
	static FeignBuilderCustomizer feignBuilderCustomizer(final String clientId, final SysManProperties sysManProperties) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(errorDecoder(clientId))
			.withRequestTimeoutsInSeconds(sysManProperties.connectTimeout(), sysManProperties.readTimeout())
			.composeCustomizersToOne();
	}

	/**
	 * The decoder both installations report their failures through.
	 *
	 * 401 and 403 are bypassed because the dispatch run tells an installation that refused our account from one that
	 * refused the call by the status alone, and without them every 4xx arrives as BAD_GATEWAY. Kept to those two, since
	 * every other 4xx is about the call we built or a row in it and is meant to count.
	 *
	 * The details path is a deep scan with concat() rather than the plain "$.details" it reads like. read(path,
	 * String.class) hands back null for an array, and a definite path throws when the field is absent, which costs the
	 * message as well. Both shapes are asserted in SysManErrorDecodingTest.
	 *
	 * @param  clientId the name the failure is reported under
	 * @return          the decoder
	 */
	static JsonPathErrorDecoder errorDecoder(final String clientId) {
		return new JsonPathErrorDecoder(clientId, List.of(UNAUTHORIZED.value(), FORBIDDEN.value()), new JsonPathSetup(TITLE_PATH, DETAIL_PATH));
	}
}
