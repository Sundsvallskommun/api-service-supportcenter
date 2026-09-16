package se.sundsvall.supportcenter.integration.sysman.configuration;

import feign.Client;
import okhttp3.Authenticator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignClientFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.supportcenter.Application;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * That the locally declared okHttpClient bean really is the one Feign ends up using for SysMan.
 *
 * Both SysMan configurations declare a bean named after the one in dept44's FeignConfiguration, which they also import,
 * and rely on the child context Spring Cloud builds per client preferring the local declaration. That is three
 * framework internals deep: whether an imported definition loses to the importing class, the order imports are
 * registered in, and whether the child bean factory allows overriding at all. It would break quietly on a Spring Cloud
 * upgrade, and the apptests would not catch it, since WireMock never answers 401 so the authenticator never runs.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class SysManFeignClientContextTest {

	@Autowired
	private FeignClientFactory feignClientFactory;

	@Test
	void theSundsvallClientCarriesItsOwnNtlmAccount() {
		assertThat(authenticatorOf(SysManSundsvallConfiguration.CLIENT_ID))
			.isInstanceOf(NTLMAuthenticator.class)
			.hasFieldOrPropertyWithValue("domain", "PERSONAL")
			.hasFieldOrPropertyWithValue("username", "sundsvallUsername");
	}

	@Test
	void theAngeClientCarriesItsOwn() {
		assertThat(authenticatorOf(SysManAngeConfiguration.CLIENT_ID))
			.isInstanceOf(NTLMAuthenticator.class)
			.hasFieldOrPropertyWithValue("domain", "ANGEDOMAIN")
			.hasFieldOrPropertyWithValue("username", "angeUsername");
	}

	/**
	 * The other half of the claim in the bean's javadoc: replacing it for these two clients leaves every other
	 * integration on the imported one.
	 */
	@Test
	void thePobClientIsLeftAlone() {
		// Asserted as the okhttp default rather than as "not the SysMan one". Two clients come from two child contexts
		// and are never the same instance, so comparing them would pass even if POB had picked up the authenticator
		// too. Authenticator.NONE is what a client nobody has touched carries.
		assertThat(authenticatorOf("pob"))
			.as("the override reached the two SysMan clients and nothing else")
			.isSameAs(Authenticator.NONE);
	}

	private Object authenticatorOf(final String clientId) {
		final var client = feignClientFactory.getInstance(clientId, Client.class);

		assertThat(client)
			.as("the client Feign resolved for %s", clientId)
			.isInstanceOf(feign.okhttp.OkHttpClient.class);

		final var delegate = (okhttp3.OkHttpClient) ReflectionTestUtils.getField(client, "delegate");

		return delegate.authenticator();
	}
}
