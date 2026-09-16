package se.sundsvall.supportcenter.integration.pob;

import generated.client.pob.PobPayload;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import se.sundsvall.supportcenter.integration.pob.configuration.EndOfLeasePOBConfiguration;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportcenter.integration.pob.EndOfLeasePOBClient.CLIENT_ID;

/**
 * The one POB call the end of lease job makes, over a client of its own.
 *
 * The same host and the same paths as {@link POBClient}, and there for two reasons. The first is its own circuit
 * breaker: the breaker on POBClient is shared by every endpoint this service exposes, so a breaker tuned to what the
 * job needs would decide availability for callers of /cases, /assets and /configuration as well. Kept apart, the job
 * can treat a POB that answers 5xx as a reason to back off without taking the API down with it, and the traffic of one
 * cannot open the breaker of the other.
 *
 * The second is its own error decoder, for the same boundary: the job has to be able to tell a POB that refused its
 * key from one that refused the call, and letting that status through on the shared decoder would change what a caller
 * of /cases or /assets is answered with. See {@link EndOfLeasePOBConfiguration}. Failures still report under the
 * POBClient integration name, which is what they are: a failure talking to POB.
 */
@FeignClient(name = CLIENT_ID, url = "${integration.pob.url}", configuration = EndOfLeasePOBConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface EndOfLeasePOBClient {

	String CLIENT_ID = "pob-end-of-lease";

	/**
	 * Returns a list of configuration-items by serialNumber.
	 *
	 * @param  pobKey       the key to use for authorization
	 * @param  serialNumber the serial number to filter the results on
	 * @return              a list of configuration-items
	 */
	@GetMapping(path = "configurationitems?Filter=SerialNumber={serialNumber}", produces = APPLICATION_JSON_VALUE)
	List<PobPayload> getConfigurationItemsBySerialNumber(@RequestHeader(AUTHORIZATION) String pobKey, @PathVariable String serialNumber);
}
