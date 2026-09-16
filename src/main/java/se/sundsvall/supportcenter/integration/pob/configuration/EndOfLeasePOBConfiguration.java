package se.sundsvall.supportcenter.integration.pob.configuration;

import feign.codec.ErrorDecoder;
import java.util.List;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static se.sundsvall.supportcenter.integration.pob.configuration.POBConfiguration.CLIENT_ID;
import static se.sundsvall.supportcenter.integration.pob.configuration.POBConfiguration.JSON_PATH_SETUP;

/**
 * What the end of lease job's POB client is configured with, kept apart from {@link POBConfiguration} for the same
 * reason the client itself is: the job needs something the endpoints this service exposes must not get.
 *
 * The same host, the same paths and the same error bodies. The one difference is the two statuses below.
 */
@Import(FeignConfiguration.class)
public class EndOfLeasePOBConfiguration {

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final POBProperties pobProperties) {
		// No encoder, unlike POBConfiguration: this client's one call is a GET with no body, so there is nothing to
		// send null values in.
		return FeignMultiCustomizer.create()
			.withErrorDecoder(errorDecoder())
			.withRequestTimeoutsInSeconds(pobProperties.connectTimeout(), pobProperties.readTimeout())
			.composeCustomizersToOne();
	}

	/**
	 * The decoder the job's failures are reported through.
	 *
	 * 401 and 403 are let through because the lookup run has to tell a POB that refused the job's key from one that
	 * refused the call it was asked to make. Without them AbstractErrorDecoder answers every 4xx with BAD_GATEWAY, and
	 * a key somebody rotated is then charged to every computer in the queue, which drains the lot into FAILED in five
	 * runs. 404 is kept from the shared decoder so the two clients read a missing item the same way. Every other 4xx
	 * is about the row the call was made for and is meant to count.
	 *
	 * Reported under the shared integration name, because that is what it is: a failure talking to POB. The name ends
	 * up in the reason written on the computer's row.
	 */
	static ErrorDecoder errorDecoder() {
		return new JsonPathErrorDecoder(CLIENT_ID, List.of(NOT_FOUND.value(), UNAUTHORIZED.value(), FORBIDDEN.value()), JSON_PATH_SETUP);
	}
}
