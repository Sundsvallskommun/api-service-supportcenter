package se.sundsvall.supportcenter.integration.sysman.configuration;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import jcifs.CIFSContext;
import jcifs.context.SingletonContext;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import jcifs.ntlmssp.av.AvPairs;
import jcifs.ntlmssp.av.AvTimestamp;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.WWW_AUTHENTICATE;

class NTLMAuthenticatorTest {

	private static final CIFSContext CIFS_CONTEXT = SingletonContext.getInstance();

	private static final String DOMAIN = "PERSONAL";
	private static final String USERNAME = "someUsername";
	private static final String PASSWORD = "somePassword";
	private static final String SCHEME_PREFIX = "NTLM ";
	// Not all zeroes. jcifs-ng drops a zero filled challenge, and the message then reads back without one.
	private static final byte[] CHALLENGE = {
		1, 2, 3, 4, 5, 6, 7, 8
	};
	// A Windows FILETIME, which is what an MsvAvTimestamp carries.
	private static final long TIMESTAMP = 133_000_000_000_000_000L;

	private MockWebServer mockWebServer;
	private OkHttpClient okHttpClient;

	@BeforeEach
	void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();
		okHttpClient = new OkHttpClient.Builder()
			.authenticator(new NTLMAuthenticator(DOMAIN, USERNAME, PASSWORD))
			.build();
	}

	@AfterEach
	void tearDown() throws IOException {
		mockWebServer.shutdown();
	}

	@Test
	void completesTheHandshake() throws Exception {
		mockWebServer.enqueue(unauthorized("NTLM"));
		mockWebServer.enqueue(unauthorized(SCHEME_PREFIX + type2Challenge()));
		mockWebServer.enqueue(new MockResponse().setResponseCode(200));

		try (final var response = call()) {
			assertThat(response.code()).isEqualTo(200);
		}

		assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
		assertThat(mockWebServer.takeRequest().getHeader(AUTHORIZATION)).isNull();
		assertThat(mockWebServer.takeRequest().getHeader(AUTHORIZATION)).startsWith(SCHEME_PREFIX);

		// The domain belongs in its own field of the type 3 message, never glued onto the username.
		final var type3Message = new Type3Message(Base64.getDecoder().decode(mockWebServer.takeRequest().getHeader(AUTHORIZATION).substring(SCHEME_PREFIX.length())));
		assertThat(type3Message.getUser()).isEqualTo(USERNAME);
		assertThat(type3Message.getDomain()).isEqualTo(DOMAIN);
		// Left to jcifs-ng the field goes out empty, and an account restricted to named machines is rejected over it.
		assertThat(type3Message.getWorkstation()).isEqualTo(CIFS_CONTEXT.getNameServiceClient().getLocalHost().getHostName());
	}

	/**
	 * What a domain joined server actually sends. The target info block carries an MsvAvTimestamp, over which jcifs-ng
	 * sends a zero filled LM response rather than computing an LMv2 one and answers with the server's own timestamp
	 * instead of the current time. A type 2 built without one takes the other branch, so nothing else here covers this.
	 */
	@Test
	void answersATypeTwoThatCarriesATimestamp() throws Exception {
		mockWebServer.enqueue(unauthorized("NTLM"));
		mockWebServer.enqueue(unauthorized(SCHEME_PREFIX + type2ChallengeWithTimestamp()));
		mockWebServer.enqueue(new MockResponse().setResponseCode(200));

		try (final var response = call()) {
			assertThat(response.code()).isEqualTo(200);
		}

		assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
		mockWebServer.takeRequest();
		mockWebServer.takeRequest();

		final var type3Message = new Type3Message(Base64.getDecoder().decode(mockWebServer.takeRequest().getHeader(AUTHORIZATION).substring(SCHEME_PREFIX.length())));
		assertThat(type3Message.getLMResponse()).hasSize(24).containsOnly((byte) 0);
		// The NTLMv2 response is a 16 byte HMAC followed by the blob, whose timestamp sits 8 bytes in.
		assertThat(ByteBuffer.wrap(type3Message.getNTResponse(), 24, 8).order(LITTLE_ENDIAN).getLong()).isEqualTo(TIMESTAMP);
	}

	@Test
	void picksTheNtlmChallengeWhenTheServerOffersMoreThanOne() throws Exception {
		mockWebServer.enqueue(new MockResponse()
			.setResponseCode(401)
			.addHeader(WWW_AUTHENTICATE, "Negotiate")
			.addHeader(WWW_AUTHENTICATE, "NTLM"));
		mockWebServer.enqueue(new MockResponse().setResponseCode(200));

		try (final var response = call()) {
			assertThat(response.code()).isEqualTo(200);
		}

		assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
		mockWebServer.takeRequest();
		assertThat(mockWebServer.takeRequest().getHeader(AUTHORIZATION)).startsWith(SCHEME_PREFIX);
	}

	/**
	 * RFC 9110 lets a server put the schemes on one line instead of one header each, and IIS is free to do either. Read
	 * as a whole value neither "Negotiate" nor "NTLM" matches, the handshake never starts, and the call fails as a naked
	 * 401 with nothing in the log saying why.
	 */
	@Test
	void picksTheNtlmChallengeWhenTheSchemesShareOneHeader() throws Exception {
		mockWebServer.enqueue(new MockResponse()
			.setResponseCode(401)
			.addHeader(WWW_AUTHENTICATE, "Negotiate, NTLM"));
		mockWebServer.enqueue(new MockResponse().setResponseCode(200));

		try (final var response = call()) {
			assertThat(response.code()).isEqualTo(200);
		}

		assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
		mockWebServer.takeRequest();
		assertThat(mockWebServer.takeRequest().getHeader(AUTHORIZATION)).startsWith(SCHEME_PREFIX);
	}

	/**
	 * A 401 that offers no NTLM at all is left as the 401 it is, for the error decoder to report.
	 */
	@Test
	void leavesA401ThatOffersNoNtlmAlone() throws Exception {
		mockWebServer.enqueue(new MockResponse()
			.setResponseCode(401)
			.addHeader(WWW_AUTHENTICATE, "Negotiate, Basic realm=\"sysman\""));

		try (final var response = call()) {
			assertThat(response.code()).isEqualTo(401);
		}

		assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
	}

	/**
	 * OkHttp records a prior response for every follow-up, a redirect included. Counting those as handshake attempts
	 * spends the budget before the challenge arrives and the call fails looking like bad credentials.
	 */
	@Test
	void countsOnlyTheUnauthorizedAnswersAsAttempts() throws Exception {
		mockWebServer.enqueue(new MockResponse().setResponseCode(302).addHeader("Location", "/moved"));
		mockWebServer.enqueue(unauthorized("NTLM"));
		mockWebServer.enqueue(unauthorized(SCHEME_PREFIX + type2Challenge()));
		mockWebServer.enqueue(new MockResponse().setResponseCode(200));

		try (final var response = call()) {
			assertThat(response.code()).isEqualTo(200);
		}

		assertThat(mockWebServer.getRequestCount()).isEqualTo(4);
	}

	@Test
	void leavesAChallengeThatIsNotNtlmAlone() throws Exception {
		mockWebServer.enqueue(unauthorized("Basic realm=\"sysman\""));

		try (final var response = call()) {
			assertThat(response.code()).isEqualTo(401);
		}

		assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
	}

	@Test
	void stopsWhenTheChallengeCannotBeRead() throws Exception {
		mockWebServer.enqueue(unauthorized(SCHEME_PREFIX + Base64.getEncoder().encodeToString("not a type 2 message".getBytes(UTF_8))));

		try (final var response = call()) {
			assertThat(response.code()).isEqualTo(401);
		}

		assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
	}

	/**
	 * A type 2 message that reads back without challenge bytes. jcifs-ng hashes the challenge to compute the NTLMv2
	 * response and throws a NullPointerException rather than an IOException when there is none, which is the reason
	 * the authenticator catches more than IOException.
	 */
	@Test
	void stopsWhenTheChallengeCarriesNoChallengeBytes() throws Exception {
		mockWebServer.enqueue(unauthorized(SCHEME_PREFIX + Base64.getEncoder().encodeToString(new Type2Message(CIFS_CONTEXT, Type2Message.getDefaultFlags(CIFS_CONTEXT), new byte[8], DOMAIN).toByteArray())));

		try (final var response = call()) {
			assertThat(response.code()).isEqualTo(401);
		}

		assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
	}

	@Test
	void givesUpWhenTheHandshakeIsRejectedOverAndOver() throws Exception {
		for (var i = 0; i < 4; i++) {
			mockWebServer.enqueue(unauthorized("NTLM"));
		}

		try (final var response = call()) {
			assertThat(response.code()).isEqualTo(401);
		}

		// The first call plus the two attempts the authenticator is allowed.
		assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
	}

	private okhttp3.Response call() throws IOException {
		return okHttpClient.newCall(new Request.Builder().url(mockWebServer.url("/api/v2/message/target")).build()).execute();
	}

	private static MockResponse unauthorized(final String challenge) {
		return new MockResponse().setResponseCode(401).addHeader(WWW_AUTHENTICATE, challenge);
	}

	/**
	 * What a server sends back.
	 */
	private static String type2Challenge() throws IOException {
		return Base64.getEncoder().encodeToString(new Type2Message(CIFS_CONTEXT, Type2Message.getDefaultFlags(CIFS_CONTEXT), CHALLENGE, DOMAIN).toByteArray());
	}

	/**
	 * The same, with the timestamp pair a domain joined server adds to its target info.
	 */
	private static String type2ChallengeWithTimestamp() throws IOException {
		final var type2Message = new Type2Message(CIFS_CONTEXT, Type2Message.getDefaultFlags(CIFS_CONTEXT), CHALLENGE, DOMAIN);
		final var targetInformation = AvPairs.decode(type2Message.getTargetInformation());
		targetInformation.add(new AvTimestamp(TIMESTAMP));
		type2Message.setTargetInformation(AvPairs.encode(targetInformation));

		return Base64.getEncoder().encodeToString(type2Message.toByteArray());
	}
}
