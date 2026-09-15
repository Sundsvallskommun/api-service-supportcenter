package se.sundsvall.supportcenter.service.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class EndOfLeaseLookupSchedulerTest {

	@Mock
	private EndOfLeaseLookupWorker endOfLeaseLookupWorkerMock;

	@InjectMocks
	private EndOfLeaseLookupScheduler endOfLeaseLookupScheduler;

	@Test
	void lookUpComputers() {
		endOfLeaseLookupScheduler.lookUpComputers();

		verify(endOfLeaseLookupWorkerMock).processComputersAwaitingLookup();
		verifyNoMoreInteractions(endOfLeaseLookupWorkerMock);
	}
}
