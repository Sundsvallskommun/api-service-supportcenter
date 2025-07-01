package se.sundsvall.supportcenter.integration.pob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.Cacheable;

import generated.client.pob.PobPayload;
import generated.client.pob.PobPayloadWithTriggerResults;
import generated.client.pob.SuspensionInfo;

@ExtendWith(MockitoExtension.class)
class POBIntegrationTest {

	private static final String POB_KEY = RandomStringUtils.secure().nextAlphabetic(10);
	private static final List<PobPayloadWithTriggerResults> POB_RESPONSE = List.of(new PobPayloadWithTriggerResults());

	@Mock
	private PobPayload payloadMock;

	@Mock
	private POBClient clientMock;

	@InjectMocks
	private POBIntegration integration;

	@AfterEach
	void verifyNoMoreMockInteractions() {
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void verifyCacheAnnotations() throws Exception {
		assertThat(POBIntegration.class.getMethod("getCaseCategories", String.class).getAnnotation(Cacheable.class).value()).containsExactly("case-categories");
		assertThat(POBIntegration.class.getMethod("getClosureCodes", String.class).getAnnotation(Cacheable.class).value()).containsExactly("closure-codes");
	}

	@Test
	void createCase() {
		when(clientMock.createCase(POB_KEY, payloadMock)).thenReturn(POB_RESPONSE);

		final var result = integration.createCase(POB_KEY, payloadMock);

		assertThat(result).isSameAs(POB_RESPONSE);
		verify(clientMock).createCase(POB_KEY, payloadMock);
	}

	@Test
	void createConfigurationItem() {
		when(clientMock.createConfigurationItem(POB_KEY, payloadMock)).thenReturn(POB_RESPONSE);

		final var result = integration.createConfigurationItem(POB_KEY, payloadMock);

		assertThat(result).isSameAs(POB_RESPONSE);
		verify(clientMock).createConfigurationItem(POB_KEY, payloadMock);
	}

	@Test
	void createItem() {
		when(clientMock.createItem(POB_KEY, payloadMock)).thenReturn(POB_RESPONSE);

		final var result = integration.createItem(POB_KEY, payloadMock);

		assertThat(result).isSameAs(POB_RESPONSE);
		verify(clientMock).createItem(POB_KEY, payloadMock);
	}

	@Test
	void deleteSuspension() {
		final var caseId = RandomStringUtils.secure().nextAlphabetic(10);

		integration.deleteSuspension(POB_KEY, caseId);

		verify(clientMock).deleteSuspension(POB_KEY, caseId);
	}

	@Test
	void getCase() {
		final var caseId = RandomStringUtils.secure().nextAlphabetic(10);
		when(clientMock.getCase(POB_KEY, caseId)).thenReturn(payloadMock);

		final var result = integration.getCase(POB_KEY, caseId);

		assertThat(result).isSameAs(payloadMock);
		verify(clientMock).getCase(POB_KEY, caseId);
	}

	@Test
	void getCaseCategories() {
		when(clientMock.getCaseCategories(POB_KEY)).thenReturn(List.of(payloadMock));

		final var result = integration.getCaseCategories(POB_KEY);

		assertThat(result).containsExactly(payloadMock);
		verify(clientMock).getCaseCategories(POB_KEY);
	}

	@Test
	void getClosureCodes() {
		when(clientMock.getClosureCodes(POB_KEY)).thenReturn(List.of(payloadMock));

		final var result = integration.getClosureCodes(POB_KEY);

		assertThat(result).containsExactly(payloadMock);
		verify(clientMock).getClosureCodes(POB_KEY);
	}

	@Test
	void getConfigurationItemsBySerialNumber() {
		final var serialNbr = RandomStringUtils.secure().nextAlphabetic(10);
		when(clientMock.getConfigurationItemsBySerialNumber(POB_KEY, serialNbr)).thenReturn(List.of(payloadMock));

		final var result = integration.getConfigurationItemsBySerialNumber(POB_KEY, serialNbr);

		assertThat(result).containsExactly(payloadMock);
		verify(clientMock).getConfigurationItemsBySerialNumber(POB_KEY, serialNbr);
	}

	@Test
	void getItemsById() {
		final var id = RandomStringUtils.secure().nextAlphabetic(10);
		when(clientMock.getItemsById(POB_KEY, id)).thenReturn(List.of(payloadMock));

		final var result = integration.getItemsById(POB_KEY, id);

		assertThat(result).containsExactly(payloadMock);
		verify(clientMock).getItemsById(POB_KEY, id);
	}

	@Test
	void getItemsByModelName() {
		final var modelName = RandomStringUtils.secure().nextAlphabetic(10);
		when(clientMock.getItemsByModelName(POB_KEY, modelName)).thenReturn(List.of(payloadMock));

		final var result = integration.getItemsByModelName(POB_KEY, modelName);

		assertThat(result).containsExactly(payloadMock);
		verify(clientMock).getItemsByModelName(POB_KEY, modelName);
	}

	@Test
	void getSuspension() {
		final var caseId = RandomStringUtils.secure().nextAlphabetic(10);
		final var suspensionInfoMock = Mockito.mock(SuspensionInfo.class);

		when(clientMock.getSuspension(POB_KEY, caseId)).thenReturn(suspensionInfoMock);

		final var result = integration.getSuspension(POB_KEY, caseId);

		assertThat(result).isSameAs(suspensionInfoMock);
		verify(clientMock).getSuspension(POB_KEY, caseId);
		verifyNoInteractions(suspensionInfoMock);
	}

	@Test
	void suspendCase() {
		final var caseId = RandomStringUtils.secure().nextAlphabetic(10);
		final var suspensionInfoMock = Mockito.mock(SuspensionInfo.class);

		integration.suspendCase(POB_KEY, caseId, suspensionInfoMock);

		verify(clientMock).suspendCase(POB_KEY, caseId, suspensionInfoMock);
		verifyNoInteractions(suspensionInfoMock);
	}

	@Test
	void updateCase() {
		integration.updateCase(POB_KEY, payloadMock);

		verify(clientMock).updateCase(POB_KEY, payloadMock);
	}

	@Test
	void updateConfigurationItem() {
		integration.updateConfigurationItem(POB_KEY, payloadMock);

		verify(clientMock).updateConfigurationItem(POB_KEY, payloadMock);
	}
}
