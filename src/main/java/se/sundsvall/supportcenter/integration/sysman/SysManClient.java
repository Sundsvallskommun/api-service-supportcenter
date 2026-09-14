package se.sundsvall.supportcenter.integration.sysman;

import generated.client.sysman.SaveMessagesToTargetsCommand;
import generated.client.sysman.TargetReference;
import java.util.List;

/**
 * What the two SysMan installations have in common, which is the shape of the calls and nothing else.
 *
 * Carries no Feign annotations on purpose. Resilience4j looks for its annotation on the method and then on the class
 * that declares it, so a call declared only here runs without a circuit breaker whatever the clients below are
 * annotated with. They therefore redeclare the calls, and a redeclared method carries its own mapping.
 */
public interface SysManClient {

	/**
	 * Sends or removes messages on a set of targets.
	 *
	 * @param  saveMessagesToTargetsCommand the targets and the messages to send or remove
	 * @return                              a reference to each target the call reached
	 */
	List<TargetReference> sendMessagesToTargets(SaveMessagesToTargetsCommand saveMessagesToTargetsCommand);
}
