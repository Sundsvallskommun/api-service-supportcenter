package se.sundsvall.supportcenter.integration.pob.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param connectTimeout the connect timeout in seconds
 * @param readTimeout    the read timeout in seconds
 * @param key            the service's own POB key. Every request over the API carries the caller's key, but the end of
 *                       lease job has no caller to take one from, so it authenticates as the service itself.
 *                       Environment specific and not checked in, and validated so that a gap is a startup failure
 *                       naming the property rather than a job that fails once a day at whatever hour it runs.
 */
@Validated
@ConfigurationProperties("integration.pob")
public record POBProperties(int connectTimeout, int readTimeout, @NotBlank String key) {
}
