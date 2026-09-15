package se.sundsvall.supportcenter.integration.sysman;

import generated.client.sysman.SaveMessagesToTargetsCommand;
import generated.client.sysman.TargetReference;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportcenter.integration.sysman.configuration.SysManProperties;
import se.sundsvall.supportcenter.integration.sysman.configuration.SysManProperties.Instance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ExtendWith(MockitoExtension.class)
class SysManIntegrationTest {

	private static final String SUNDSVALL_MUNICIPALITY_ID = "2281";
	private static final String ANGE_MUNICIPALITY_ID = "2260";

	@Mock
	private SysManSundsvallClient sysManSundsvallClientMock;

	@Mock
	private SysManAngeClient sysManAngeClientMock;

	private SysManIntegration sysManIntegration;

	@BeforeEach
	void setUp() {
		final var sysManProperties = new SysManProperties(1, 2,
			new Instance(SUNDSVALL_MUNICIPALITY_ID, "http://sundsvall.url", "PERSONAL", "sundsvallUsername", "sundsvallPassword"),
			new Instance(ANGE_MUNICIPALITY_ID, "http://ange.url", "ANGE", "angeUsername", "angePassword"));

		sysManIntegration = new SysManIntegration(sysManProperties, sysManSundsvallClientMock, sysManAngeClientMock);
	}

	@Test
	void sendMessagesToTargetsForSundsvall() {
		final var command = new SaveMessagesToTargetsCommand();
		final var targetReferences = List.of(new TargetReference());

		when(sysManSundsvallClientMock.sendMessagesToTargets(command)).thenReturn(targetReferences);

		final var result = sysManIntegration.sendMessagesToTargets(SUNDSVALL_MUNICIPALITY_ID, command);

		assertThat(result).isSameAs(targetReferences);
		verify(sysManSundsvallClientMock).sendMessagesToTargets(command);
		verifyNoMoreInteractions(sysManSundsvallClientMock);
		verifyNoInteractions(sysManAngeClientMock);
	}

	@Test
	void sendMessagesToTargetsForAnge() {
		final var command = new SaveMessagesToTargetsCommand();
		final var targetReferences = List.of(new TargetReference());

		when(sysManAngeClientMock.sendMessagesToTargets(command)).thenReturn(targetReferences);

		final var result = sysManIntegration.sendMessagesToTargets(ANGE_MUNICIPALITY_ID, command);

		assertThat(result).isSameAs(targetReferences);
		verify(sysManAngeClientMock).sendMessagesToTargets(command);
		verifyNoMoreInteractions(sysManAngeClientMock);
		verifyNoInteractions(sysManSundsvallClientMock);
	}

	@Test
	void sendMessagesToTargetsForAMunicipalityWithoutAnInstallation() {
		final var command = new SaveMessagesToTargetsCommand();

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> sysManIntegration.sendMessagesToTargets("1984", command))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
				assertThat(problem.getDetail()).isEqualTo("No SysMan installation is configured for municipality 1984");
			});

		verifyNoInteractions(sysManSundsvallClientMock, sysManAngeClientMock);
	}

	/**
	 * Map.of answers the same mistake with a bare duplicate key and no hint as to which property to look at, which is
	 * out of step with every other gap in this configuration.
	 */
	@Test
	void twoInstallationsOnTheSameMunicipalityNameTheProperties() {
		final var sysManProperties = new SysManProperties(1, 2,
			new Instance(SUNDSVALL_MUNICIPALITY_ID, "http://sundsvall.url", "PERSONAL", "sundsvallUsername", "sundsvallPassword"),
			new Instance(SUNDSVALL_MUNICIPALITY_ID, "http://ange.url", "ANGEDOMAIN", "angeUsername", "angePassword"));

		assertThatExceptionOfType(IllegalStateException.class)
			.isThrownBy(() -> new SysManIntegration(sysManProperties, sysManSundsvallClientMock, sysManAngeClientMock))
			.withMessageContaining("integration.sysman.sundsvall.municipalityId")
			.withMessageContaining("integration.sysman.ange.municipalityId");
	}
}
