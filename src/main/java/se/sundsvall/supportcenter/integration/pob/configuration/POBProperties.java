package se.sundsvall.supportcenter.integration.pob.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param connectTimeout the connect timeout in seconds
 * @param readTimeout    the read timeout in seconds
 * @param key            the service's own POB key. Every request over the API carries the caller's key, but the end of
 *                       lease job has no caller to take one from, so it authenticates as the service itself.
 *                       Environment specific and not checked in.
 *                       <p>
 *                       Deliberately not validated. Only the end of lease job needs it, and that job is off unless an
 *                       environment turns it on, so a missing key must not stop a service whose API has never needed
 *                       one from starting at all. The job checks for it itself and says so on its health indicator.
 */
@ConfigurationProperties("integration.pob")
public record POBProperties(int connectTimeout, int readTimeout, String key) {
}
