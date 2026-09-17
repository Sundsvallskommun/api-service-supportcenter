package se.sundsvall.supportcenter.integration.sysman.configuration;

import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import jcifs.CIFSContext;
import jcifs.context.SingletonContext;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.WWW_AUTHENTICATE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Answers an NTLM challenge on behalf of an OkHttp client.
 *
 * NTLM is three messages bound to one connection rather than a header that can be set up front, which is why this is
 * an authenticator and not a request interceptor. The server answers the first request with a bare "NTLM", we send a
 * type 1 message, the server answers with its type 2 challenge, and we answer that with a type 3 message carrying the
 * response computed from the password.
 */
class NTLMAuthenticator implements Authenticator {

	private static final Logger LOG = LoggerFactory.getLogger(NTLMAuthenticator.class);

	private static final String SCHEME = "NTLM";
	private static final String SCHEME_PREFIX = SCHEME + " ";

	/**
	 * The handshake is two answers, one per message we send. OkHttp calls an authenticator again for every 401, so
	 * without a ceiling a rejected type 3 message starts the handshake over for as long as the server keeps saying no.
	 */
	private static final int MAX_ATTEMPTS = 2;

	private final String domain;
	private final String username;
	private final String password;
	private final CIFSContext cifsContext;
	private final String workstation;
	private final String type1Message;

	NTLMAuthenticator(final String domain, final String username, final String password) {
		this.domain = domain;
		this.username = username;
		this.password = password;
		// One shared context: each one registers its own JVM shutdown hook. getInstance returns null rather than
		// throwing when a jcifs.* property will not parse, and the null check is what names that cause.
		this.cifsContext = requireNonNull(SingletonContext.getInstance(), "jcifs could not build its context, check the jcifs.* properties");
		// The host we authenticate from. Left empty, a domain controller turns away an account carrying a "Log On To"
		// restriction. jcifs.netbios.hostname when set, otherwise a name generated from the local address.
		this.workstation = cifsContext.getNameServiceClient().getLocalHost().getHostName();
		this.type1Message = Base64.getEncoder().encodeToString(new Type1Message(cifsContext, Type1Message.getDefaultFlags(cifsContext), null, null).toByteArray());
	}

	@Override
	public Request authenticate(final Route route, final Response response) {
		if (attempts(response) > MAX_ATTEMPTS) {
			LOG.warn("Gave up the NTLM handshake against {} after {} attempts. Letting the 401 through to the error decoder", response.request().url().host(), MAX_ATTEMPTS);
			return null;
		}

		return challenge(response)
			.flatMap(this::answer)
			.map(message -> response.request().newBuilder().header(AUTHORIZATION, SCHEME_PREFIX + message).build())
			.orElse(null);
	}

	/**
	 * The NTLM challenge among whatever else the server offers. A server that advertises Negotiate as well may answer
	 * with one header per scheme or with the schemes on a single comma separated line, which RFC 9110 allows, and
	 * picking the first value blindly hands the wrong scheme to the parser below.
	 *
	 * Splitting on the comma is safe for the value we want: a type 2 challenge is base64, whose alphabet has no comma.
	 */
	private Optional<String> challenge(final Response response) {
		final var values = response.headers().values(WWW_AUTHENTICATE);

		final var challenge = values.stream()
			.flatMap(value -> Arrays.stream(value.split(",")))
			.map(String::trim)
			.filter(value -> SCHEME.equalsIgnoreCase(value) || value.regionMatches(true, 0, SCHEME_PREFIX, 0, SCHEME_PREFIX.length()))
			.findFirst();

		if (challenge.isEmpty()) {
			// Otherwise the call fails as a naked 401 with no line saying the handshake never started.
			LOG.debug("The 401 from {} offered no NTLM challenge, only {}", response.request().url().host(), values);
		}

		return challenge;
	}

	/**
	 * The type 1 message while the server has only named the scheme, and the type 3 answer once it has sent its type 2
	 * challenge.
	 */
	private Optional<String> answer(final String challenge) {
		if (challenge.length() <= SCHEME.length()) {
			return ofNullable(type1Message);
		}

		try {
			final var type2Message = new Type2Message(Base64.getDecoder().decode(challenge.substring(SCHEME_PREFIX.length())));
			// No flags of our own. jcifs-ng ORs in the defaults it reads off the type 2, which is where the choice
			// between unicode and OEM comes from, and anything we passed could only add bits on top of that.
			// The null is the target SPN, carried as MsvAvTargetName and insisted on only by an IIS site with Extended
			// Protection set to Required.
			return Optional.of(Base64.getEncoder().encodeToString(
				new Type3Message(cifsContext, type2Message, null, password, domain, username, workstation, 0).toByteArray()));
		} catch (final Exception e) {
			// Broad on purpose. Bad base64 is an IllegalArgumentException, a malformed message an IOException, and a
			// well formed type 2 with no challenge bytes a NullPointerException out of the digest, since jcifs computes
			// an NTLMv2 response by default and hashes the challenge. None is something OkHttp catches, so an escaping
			// one leaves the call as itself rather than as the 401 the server actually sent.
			LOG.error("Could not answer the NTLM type 2 challenge, so no type 3 message was sent", e);
			return empty();
		}
	}

	/**
	 * How many times we have answered this exchange, counted over the responses that led here. Only the 401s count:
	 * OkHttp records a prior response for every follow-up, and a redirect on the way in would otherwise spend the
	 * handshake before the challenge even arrives.
	 */
	private static int attempts(final Response response) {
		var attempts = 1;
		for (var prior = response.priorResponse(); prior != null; prior = prior.priorResponse()) {
			if (prior.code() == UNAUTHORIZED.value()) {
				attempts++;
			}
		}
		return attempts;
	}
}
