package se.sundsvall.supportcenter.integration.sysman.configuration;

import java.util.Optional;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import jcifs.util.Base64;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private final String type1Message;

	NTLMAuthenticator(final String domain, final String username, final String password) {
		this.domain = domain;
		this.username = username;
		this.password = password;
		this.type1Message = Base64.encode(new Type1Message(Type1Message.getDefaultFlags(), null, null).toByteArray());
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
	 * The NTLM challenge among whatever else the server offers. A server that advertises Negotiate as well answers with
	 * more than one header, and picking the first one blindly hands the wrong scheme to the parser below.
	 */
	private Optional<String> challenge(final Response response) {
		return response.headers().values(WWW_AUTHENTICATE).stream()
			.filter(value -> SCHEME.equalsIgnoreCase(value) || value.regionMatches(true, 0, SCHEME_PREFIX, 0, SCHEME_PREFIX.length()))
			.findFirst();
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
			final var type2Message = new Type2Message(Base64.decode(challenge.substring(SCHEME_PREFIX.length())));
			// Flags read off the type 2 rather than the no argument defaults, which are OR'd into whatever the server
			// negotiated and can only add bits. Against a server that asked for OEM the no argument version forces
			// unicode back on, and jcifs then writes the username as UTF-16LE for a server that cannot read it.
			return ofNullable(Base64.encode(new Type3Message(type2Message, password, domain, username, null, Type3Message.getDefaultFlags(type2Message)).toByteArray()));
		} catch (final Exception e) {
			// Broad on purpose. A malformed message is an IOException, but a well formed type 2 that carries no
			// challenge bytes throws a NullPointerException out of the digest instead, since jcifs computes an NTLMv2
			// response by default and hashes the challenge. Neither is something OkHttp catches, so an escaping one
			// leaves the call as itself rather than as the 401 the server actually sent.
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
