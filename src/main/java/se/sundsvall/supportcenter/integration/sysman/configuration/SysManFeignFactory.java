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
			.withErrorDecoder(new JsonPathErrorDecoder(clientId, new JsonPathSetup("$.message", "$.details")))
			.withRequestTimeoutsInSeconds(sysManProperties.connectTimeout(), sysManProperties.readTimeout())
			.composeCustomizersToOne();
	}
}
