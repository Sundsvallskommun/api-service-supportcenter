package se.sundsvall.supportcenter.integration.sysman.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;

/**
 * The two SysMan installations we talk to. Same API, one run by us and one by Ånge, each with its own host and its own
 * account.
 *
 * Validated so that a gap is a startup failure naming the property rather than a handshake that fails against the
 * installation once a day, at whatever hour the batch happens to be picked up.
 *
 * The opposite policy to {@link se.sundsvall.supportcenter.integration.pob.configuration.POBProperties#key()}, which is
 * left unvalidated so that a service whose API has never needed one still starts without it. The difference is where
 * the value is read. The POB key is a header on a client that is already built, while url is a {@code @FeignClient}
 * attribute, so the client bean cannot be built without it at all. Absent, the placeholder reaches URI parsing
 * unresolved and the context dies on an "Illegal character in authority" that quotes the placeholder back. Blank, the
 * client falls through to load balancing and dies on a missing LoadBalancerClientFactory. Both installations are load
 * bearing for startup either way, so validating them decides only whether the failure names the property. See the
 * deployment note in README.md.
 *
 * @param connectTimeout the connect timeout in seconds, shared by both installations
 * @param readTimeout    the read timeout in seconds, shared by both installations
 * @param sundsvall      our own installation
 * @param ange           the installation in Ånge
 */
@Validated
@ConfigurationProperties("integration.sysman")
public record SysManProperties(

	@Positive int connectTimeout,

	@Positive int readTimeout,

	@Valid @NotNull Instance sundsvall,

	@Valid @NotNull Instance ange) {

	/**
	 * One SysMan installation.
	 *
	 * @param municipalityId the municipality whose computers belong to this installation. Read from the POB
	 *                       configuration item rather than from the caller, and kept here so that which of the two
	 *                       installations it goes to is configuration. A municipality outside MUNICIPALITY_MAP is a
	 *                       different question and still needs a release.
	 * @param url            the base url of the installation
	 * @param domain         the NTLM domain, which is its own field in the handshake and never a prefix on the username
	 * @param username       the account to authenticate as, without a domain prefix
	 * @param password       the password of the account
	 */
	public record Instance(

		@ValidMunicipalityId String municipalityId,

		@NotBlank String url,

		@NotBlank String domain,

		@NotBlank String username,

		@NotBlank String password) {

		/**
		 * Overridden because a record prints every component it has, and this one carries an NTLM password. Nothing in
		 * the service logs an Instance today, but a stray LOG.debug or a binding failure that prints the bound value is
		 * one line away. Actuator needs no help here, configprops.show-values defaults to NEVER.
		 */
		@Override
		public String toString() {
			return "Instance[municipalityId=%s, url=%s, domain=%s, username=%s, password=%s]"
				.formatted(municipalityId, url, domain, username, password == null ? null : "*****");
		}
	}
}
