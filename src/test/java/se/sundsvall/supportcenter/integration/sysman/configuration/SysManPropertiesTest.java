package se.sundsvall.supportcenter.integration.sysman.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.supportcenter.Application;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class SysManPropertiesTest {

	@Autowired
	private SysManProperties sysManProperties;

	/**
	 * A record prints every component it has, and this one carries an NTLM password. Nothing logs an Instance today, but
	 * a stray LOG.debug or a binding failure that prints the bound value is one line away.
	 */
	@Test
	void theInstanceKeepsThePasswordOutOfItsToString() {
		assertThat(sysManProperties.sundsvall().toString())
			.contains("sundsvallUsername")
			.contains("PERSONAL")
			.doesNotContain("sundsvallPassword")
			.contains("password=*****");
	}

	@Test
	void testProperties() {
		assertThat(sysManProperties.connectTimeout()).isEqualTo(10);
		assertThat(sysManProperties.readTimeout()).isEqualTo(30);

		assertThat(sysManProperties.sundsvall()).satisfies(instance -> {
			assertThat(instance.municipalityId()).isEqualTo("2281");
			assertThat(instance.url()).isEqualTo("http://sysman.sundsvall.url");
			assertThat(instance.domain()).isEqualTo("PERSONAL");
			assertThat(instance.username()).isEqualTo("sundsvallUsername");
			assertThat(instance.password()).isEqualTo("sundsvallPassword");
		});

		assertThat(sysManProperties.ange()).satisfies(instance -> {
			assertThat(instance.municipalityId()).isEqualTo("2260");
			assertThat(instance.url()).isEqualTo("http://sysman.ange.url");
			assertThat(instance.domain()).isEqualTo("ANGEDOMAIN");
			assertThat(instance.username()).isEqualTo("angeUsername");
			assertThat(instance.password()).isEqualTo("angePassword");
		});
	}
}
