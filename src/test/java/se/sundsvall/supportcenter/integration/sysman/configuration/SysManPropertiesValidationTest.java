package se.sundsvall.supportcenter.integration.sysman.configuration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import se.sundsvall.supportcenter.Application;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * The point of validating the properties is that a gap stops the service at startup and names what is missing, rather
 * than surfacing as a failed handshake once a day at whatever hour the batch is picked up.
 *
 * Starts the application rather than binding the record on its own. Binding it on its own passes just as well with
 * {@code @Validated} taken off the record or {@code @Valid} taken off an installation, since nothing then asks a
 * validator anything.
 */
class SysManPropertiesValidationTest {

	@ParameterizedTest
	@CsvSource(delimiter = ';', value = {
		"integration.sysman.sundsvall.password=; must not be blank",
		"integration.sysman.ange.municipalityId=notAMunicipality; not a valid municipality ID",
		"integration.sysman.connectTimeout=0; must be greater than 0"
	})
	void refusesToStartOnAGap(final String override, final String reason) {
		assertThatExceptionOfType(Exception.class)
			.isThrownBy(() -> new SpringApplicationBuilder(Application.class)
				.profiles("junit")
				.web(WebApplicationType.NONE)
				// As a command line argument rather than through properties(), which lands in the default property
				// source and loses to application.yml.
				.run("--" + override)
				.close())
			.withStackTraceContaining(override.substring(0, override.indexOf('=')))
			.withStackTraceContaining(reason);
	}
}
