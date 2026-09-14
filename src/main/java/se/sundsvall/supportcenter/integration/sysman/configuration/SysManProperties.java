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
	 *                       configuration item rather than from the caller, and kept here so that the routing table is
	 *                       configuration rather than a constant in the code.
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
	}
}
