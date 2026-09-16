package se.sundsvall.supportcenter.integration.pob.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.jackson.JacksonEncoder;
import java.util.List;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder.JsonPathSetup;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Import(FeignConfiguration.class)
public class POBConfiguration {

	public static final String CLIENT_ID = "pob";

	/**
	 * Shared with {@link EndOfLeasePOBConfiguration}, which decodes the same POB errors and differs only in which
	 * statuses it lets through. JsonPath below is constructed to only extract values from the attributes if they exist.
	 * UserMessage and Message should never exist at the same time (according to API-spec).
	 */
	static final JsonPathSetup JSON_PATH_SETUP = new JsonPathSetup(
		"concat($[?(@.UserMessage != null)].UserMessage, $[?(@.Message != null)].Message)",
		"concat($[?(@.InternalMessage != null)].InternalMessage)");

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final POBProperties pobProperties) {
		return FeignMultiCustomizer.create()
			.withEncoder(encoder())
			.withErrorDecoder(errorDecoder())
			.withRequestTimeoutsInSeconds(pobProperties.connectTimeout(), pobProperties.readTimeout())
			.composeCustomizersToOne();
	}

	private Encoder encoder() {
		// Feign must be able to send null values.
		return new JacksonEncoder(new ObjectMapper().setDefaultPropertyInclusion(ALWAYS));
	}

	private ErrorDecoder errorDecoder() {
		// 404:s should be thrown as 404:s and not 502:s
		// Left at that on purpose. This decoder serves every endpoint this service exposes, so letting a POB 401
		// through here would change what a caller of /cases or /assets is answered with. The end of lease job needs
		// that distinction and has its own decoder for it.
		return new JsonPathErrorDecoder(CLIENT_ID, List.of(NOT_FOUND.value()), JSON_PATH_SETUP);
	}
}
