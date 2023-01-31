package se.sundsvall.supportcenter.service.util;

import static com.google.gson.FieldNamingPolicy.UPPER_CAMEL_CASE;
import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS;
import static java.util.Objects.nonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import generated.client.pob.PobPayload;

public class CaseUtil {

	private static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(UPPER_CAMEL_CASE).create();

	private CaseUtil() {}

	/**
	 * Checks if the key denoted by the provided jsonPath exists and has a value that is not null.
	 * 
	 * @param pobPayload
	 * @param jsonPath
	 * @return true if the key exist and has a non null value.
	 */
	public static boolean jsonPathExists(PobPayload pobPayload, String jsonPath) {
		final var parsedJson = parse(GSON.toJson(pobPayload), defaultConfiguration().addOptions(SUPPRESS_EXCEPTIONS));
		return nonNull(parsedJson.read(jsonPath));
	}

	/**
	 * Extracts the value denoted by the provided jsonPath.
	 * 
	 * @param pobPayload
	 * @param jsonPath
	 * @param suppressExceptions whether to suppress exceptions for invalid paths, or not.
	 * @return the value that corresponds to the provided jsonPath.
	 */
	public static String extractValueFromJsonPath(PobPayload pobPayload, String jsonPath, boolean suppressExceptions) {
		final var parsedJson = parse(GSON.toJson(pobPayload), suppressExceptions ? defaultConfiguration().addOptions(SUPPRESS_EXCEPTIONS) : defaultConfiguration());
		return parsedJson.read(jsonPath);
	}
}
