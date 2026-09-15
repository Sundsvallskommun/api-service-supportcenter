package se.sundsvall.supportcenter.integration.sysman.configuration;

import feign.Client;
import feign.okhttp.OkHttpClient;
import javax.net.ssl.X509TrustManager;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder.JsonPathSetup;
import se.sundsvall.dept44.security.Truststore;
import se.sundsvall.supportcenter.integration.sysman.configuration.SysManProperties.Instance;

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
	 * @param  instance   the installation whose account answers the challenge
	 * @param  truststore the certificates the process trusts
	 * @return            the client
	 */
	static Client okHttpClient(final Instance instance, final Truststore truststore) {
		final var trustManager = (X509TrustManager) truststore.getTrustManagerFactory().getTrustManagers()[0];

		return new OkHttpClient(new okhttp3.OkHttpClient.Builder()
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
			// SysMan answers a failure with an ApiErrorMessage, whose message says what went wrong and whose details
			// carry whatever more the server had to say.
			//
			// The details path is a deep scan with concat() rather than the plain "$.details" it reads like, and both
			// halves of that are deliberate. read(path, String.class) hands back null for an array, so the plain path
			// drops the details on every error that has any. A definite path also throws PathNotFoundException when
			// the field is absent, and AbstractErrorDecoder answers any throw from here by giving up on the whole body
			// and reporting "Unknown error", which loses the message as well. A deep scan is indefinite and answers an
			// absent field with nothing instead of throwing. Measured against every shape SysMan can send: an array, an
			// empty array, an absent field, an explicit null, a plain string, and an html error page from something in
			// front of it.
			.withErrorDecoder(new JsonPathErrorDecoder(clientId, new JsonPathSetup(TITLE_PATH, DETAIL_PATH)))
			.withRequestTimeoutsInSeconds(sysManProperties.connectTimeout(), sysManProperties.readTimeout())
			.composeCustomizersToOne();
	}
}
