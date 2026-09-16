package se.sundsvall.supportcenter.integration.sysman.configuration;

import feign.Request;
import feign.Response;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder;
import se.sundsvall.dept44.exception.ClientProblem;
import se.sundsvall.dept44.exception.ServerProblem;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static se.sundsvall.supportcenter.integration.sysman.configuration.SysManFeignFactory.errorDecoder;

/**
 * What a failed SysMan call is reported as.
 *
 * The reason ends up on the computer's row and in the log, so it is the only thing a person has to go on when the queue
 * stops moving. AbstractErrorDecoder answers any throw out of the json paths by giving up on the body and saying
 * "Unknown error", which loses the message too, so every shape SysMan can answer with is asserted here rather than
 * assumed.
 *
 * The status matters just as much, because the dispatch run reads it to decide whether a failure is the installation
 * refusing our account or refusing the call, and the two are charged to entirely different parties. The decoder is
 * taken from the factory that builds the production one rather than constructed here, so that what is asserted is what
 * the clients are actually wired with.
 */
class SysManErrorDecodingTest {

	private static final String CLIENT_ID = "sysman-sundsvall";

	private final JsonPathErrorDecoder errorDecoder = errorDecoder(CLIENT_ID);

	static Stream<Arguments> bodies() {
		return of(
			Arguments.of("an array of details", """
				{"message":"Target not found","details":["no such computer","check the name"]}""", "no such computer"),
			Arguments.of("an empty array", """
				{"message":"Unauthorized","details":[]}""", null),
			Arguments.of("no details at all", """
				{"message":"Something broke"}""", null),
			Arguments.of("an explicit null", """
				{"message":"Something broke","details":null}""", null),
			Arguments.of("details as a plain string", """
				{"message":"Something broke","details":"the name is too long"}""", "the name is too long"));
	}

	/**
	 * Whatever the shape, the message SysMan sent has to survive. Losing it is the failure this guards against.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("bodies")
	void theMessageAlwaysSurvives(final String name, final String body, final String expectedDetail) {
		final var message = decode(body);

		assertThat(message)
			.doesNotContain("Unknown error")
			.contains("title=");

		if (expectedDetail != null) {
			assertThat(message).contains(expectedDetail);
		}
	}

	/**
	 * Something in front of SysMan answering with an html error page is the one case no json path can save, since the
	 * message is not there to be read. The decoder still has to come back with the status rather than break, and the
	 * dispatch run counts it like any other call the installation turned down.
	 */
	@Test
	void aBodyThatIsNotJsonFallsBackOnTheStatus() {
		final var message = decode("<html>502 Bad Gateway</html>");

		assertThat(message)
			.contains(CLIENT_ID)
			.contains("400 Bad Request");
	}

	/**
	 * The details are the part a plain "$.details" drops, since read(path, String.class) hands back null for an array.
	 */
	@Test
	void theDetailsOfAnArrayAreKept() {
		final var message = decode("""
			{"message":"Target not found","details":["no such computer","check the name"]}""");

		assertThat(message)
			.contains("Target not found")
			.contains("no such computer")
			.contains("check the name");
	}

	/**
	 * A definite path throws when the field is absent, and a throw here costs the message as well.
	 */
	@Test
	void anErrorWithoutDetailsKeepsItsMessage() {
		final var message = decode("""
			{"message":"Something broke"}""");

		assertThat(message)
			.contains("Something broke")
			.doesNotContain("Unknown error");
	}

	/**
	 * The branch in the dispatch run that spares a whole municipality its attempts turns on this status alone, and an
	 * account SysMan turns down is the one 4xx it must recognize. Asserted against the decoder rather than against a
	 * ClientProblem written by hand, since a hand-written one passes whatever the decoder does.
	 */
	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, names = {
		"UNAUTHORIZED", "FORBIDDEN"
	})
	void aRejectedAccountKeepsItsOwnStatus(final HttpStatus status) {
		assertThat(decode(status.value(), """
			{"message":"Access denied"}"""))
			.isInstanceOf(ClientProblem.class)
			.extracting("status")
			.isEqualTo(status);
	}

	/**
	 * The other half of that branch. Every 4xx that is not the account is about the call we built or a row in it, and
	 * has to keep costing the computers in it an attempt, so it must not arrive looking like a rejected account.
	 */
	@Test
	void everyOtherClientErrorIsStillReportedAsBadGateway() {
		assertThat(decode(BAD_REQUEST.value(), """
			{"message":"Invalid target name"}"""))
			.isInstanceOf(ClientProblem.class)
			.extracting("status")
			.isEqualTo(BAD_GATEWAY);
	}

	/**
	 * An installation that is unwell is caught a branch earlier and is spared the attempts too, which rests on the
	 * series deciding the exception rather than the status.
	 */
	@Test
	void anUnwellInstallationIsAServerProblem() {
		assertThat(decode(INTERNAL_SERVER_ERROR.value(), """
			{"message":"Something broke"}"""))
			.isInstanceOf(ServerProblem.class);
	}

	private String decode(final String body) {
		return decode(BAD_REQUEST.value(), body).getMessage();
	}

	private Exception decode(final int status, final String body) {
		final var request = Request.create(Request.HttpMethod.POST, "http://sysman.url/api/v2/message/target", Map.of(), null, null, null);
		final var response = Response.builder()
			.status(status)
			.reason(HttpStatus.valueOf(status).getReasonPhrase())
			.request(request)
			.body(body, UTF_8)
			.build();

		return errorDecoder.decode("SysManClient#sendMessagesToTargets(SaveMessagesToTargetsCommand)", response);
	}
}
