package se.sundsvall.supportcenter.integration.sysman.configuration;

import feign.Client;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.security.Truststore;

@Import(FeignConfiguration.class)
public class SysManAngeConfiguration {

	public static final String CLIENT_ID = "sysman-ange";

	/**
	 * Named after the bean in FeignConfiguration on purpose. Declaring it here replaces the imported one for this
	 * client only, which is what adds the NTLM authenticator without touching the other integrations.
	 *
	 * @param  sysManProperties the installations
	 * @param  truststore       the certificates the process trusts
	 * @return                  the client
	 */
	@Bean
	Client okHttpClient(final SysManProperties sysManProperties, final Truststore truststore) {
		return SysManFeignFactory.okHttpClient(sysManProperties.ange(), truststore);
	}

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final SysManProperties sysManProperties) {
		return SysManFeignFactory.feignBuilderCustomizer(CLIENT_ID, sysManProperties);
	}
}
