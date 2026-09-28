package se.sundsvall.supportcenter.integration.sysman.configuration;

import com.jayway.jsonpath.JsonPath;
import feign.Response;
import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder;

import static java.util.Collections.emptyList;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * A JsonPathErrorDecoder that also reads an error whose JSON body is a bare string.
 *
 * SysMan answers a missing permission with "Missing permission on resource: Message.Send" as the whole body rather than
 * inside an ApiErrorMessage. The title path throws on a string, and AbstractErrorDecoder answers any throw with
 * "Unknown error", so the one line saying what to fix was lost from the row, the log and the health endpoint alike.
 *
 * Kept to a JSON content type. JsonSmart parses anything that is neither an object nor an array as a string, an html
 * error page from something in front of SysMan included, and a page of markup is no message.
 */
final class SysManErrorDecoder extends JsonPathErrorDecoder {

	/**
	 * Reads the whole body as the title. A second decoder rather than a message built here, since the format of the
	 * message is private to dept44 and every failure should read the same.
	 */
	private final JsonPathErrorDecoder bareStringDecoder;

	SysManErrorDecoder(final String integrationName, final List<Integer> bypassResponseCodes, final JsonPathSetup jsonPathSetup) {
		super(integrationName, bypassResponseCodes, jsonPathSetup);
		this.bareStringDecoder = new JsonPathErrorDecoder(integrationName, new JsonPathSetup("$"));
	}

	@Override
	public String extractErrorMessage(final Response response) throws IOException {
		return isBareJsonString(response) ? bareStringDecoder.extractErrorMessage(response) : super.extractErrorMessage(response);
	}

	/**
	 * The body is looked at before the header, so that an ApiErrorMessage never depends on a content type that parses.
	 */
	private boolean isBareJsonString(final Response response) throws IOException {
		return JsonPath.parse(bodyAsString(response)).json() instanceof String && isJson(response);
	}

	private static boolean isJson(final Response response) {
		return response.headers().getOrDefault(CONTENT_TYPE, emptyList()).stream()
			.map(MediaType::parseMediaType)
			.anyMatch(APPLICATION_JSON::isCompatibleWith);
	}
}
