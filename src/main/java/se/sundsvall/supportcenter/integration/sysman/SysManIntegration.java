package se.sundsvall.supportcenter.integration.sysman;

import generated.client.sysman.SaveMessagesToTargetsCommand;
import generated.client.sysman.TargetReference;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportcenter.integration.sysman.configuration.SysManProperties;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Sends to the SysMan installation a computer belongs to.
 *
 * Which installation that is follows from the municipality on the POB configuration item, not from the municipality
 * the batch was sent to. The two are read from configuration rather than written here, so moving a municipality from
 * one of these installations to the other is a property and not a release.
 *
 * Taking on a municipality neither of them covers is not. The POB field is read against MUNICIPALITY_MAP, which knows
 * 2281 and 2260 and nothing else, so anything else never reaches this class to be routed.
 */
@Component
public class SysManIntegration {

	private static final String NO_INSTANCE = "No SysMan installation is configured for municipality %s";
	private static final String SAME_MUNICIPALITY = "integration.sysman.sundsvall.municipalityId and integration.sysman.ange.municipalityId are both %s, so there is no telling which installation a computer belongs to";

	private final Map<String, SysManClient> clientsByMunicipalityId;

	public SysManIntegration(
		final SysManProperties sysManProperties,
		final SysManSundsvallClient sysManSundsvallClient,
		final SysManAngeClient sysManAngeClient) {

		this.clientsByMunicipalityId = routingTable(sysManProperties, sysManSundsvallClient, sysManAngeClient);
	}

	/**
	 * The municipality of a computer to the installation it belongs to.
	 *
	 * The two municipalities are checked against each other rather than left to Map.of, which answers the same mistake
	 * with a bare duplicate key and no hint as to which property to look at. Every other gap in this configuration is
	 * reported by name, and this one should be too.
	 */
	private static Map<String, SysManClient> routingTable(
		final SysManProperties sysManProperties,
		final SysManSundsvallClient sysManSundsvallClient,
		final SysManAngeClient sysManAngeClient) {

		final var sundsvallMunicipalityId = sysManProperties.sundsvall().municipalityId();
		final var angeMunicipalityId = sysManProperties.ange().municipalityId();

		if (sundsvallMunicipalityId.equals(angeMunicipalityId)) {
			throw new IllegalStateException(SAME_MUNICIPALITY.formatted(sundsvallMunicipalityId));
		}

		return Map.of(sundsvallMunicipalityId, sysManSundsvallClient, angeMunicipalityId, sysManAngeClient);
	}

	/**
	 * @param  municipalityId               the municipality of the computer, which decides the installation
	 * @param  saveMessagesToTargetsCommand the targets and the messages to send or remove
	 * @return                              a reference to each target the call reached
	 */
	public List<TargetReference> sendMessagesToTargets(final String municipalityId, final SaveMessagesToTargetsCommand saveMessagesToTargetsCommand) {
		return client(municipalityId).sendMessagesToTargets(saveMessagesToTargetsCommand);
	}

	private SysManClient client(final String municipalityId) {
		return ofNullable(clientsByMunicipalityId.get(municipalityId))
			// Our own routing configuration is short of a municipality. Nobody asked us for this one, the scheduler
			// read it off a POB configuration item, so the fault is on this side and not on a caller's.
			.orElseThrow(() -> Problem.valueOf(INTERNAL_SERVER_ERROR, NO_INSTANCE.formatted(municipalityId)));
	}
}
