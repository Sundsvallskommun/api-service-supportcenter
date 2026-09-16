package se.sundsvall.supportcenter.integration.sysman;

import generated.client.sysman.SaveMessagesToTargetsCommand;
import generated.client.sysman.TargetReference;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.supportcenter.integration.sysman.configuration.SysManSundsvallConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportcenter.integration.sysman.configuration.SysManSundsvallConfiguration.CLIENT_ID;

@FeignClient(name = CLIENT_ID, url = "${integration.sysman.sundsvall.url}", configuration = SysManSundsvallConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface SysManSundsvallClient extends SysManClient {

	@Override
	@PostMapping(path = "api/v2/message/target", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	List<TargetReference> sendMessagesToTargets(@RequestBody SaveMessagesToTargetsCommand saveMessagesToTargetsCommand);
}
