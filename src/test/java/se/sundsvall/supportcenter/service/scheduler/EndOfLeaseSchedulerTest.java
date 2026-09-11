package se.sundsvall.supportcenter.service.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class EndOfLeaseSchedulerTest {

	@Mock
	private EndOfLeaseSchedulerWorker endOfLeaseSchedulerWorkerMock;

	@InjectMocks
	private EndOfLeaseScheduler endOfLeaseScheduler;

	@Test
	void reportPendingComputers() {
		endOfLeaseScheduler.reportPendingComputers();

		verify(endOfLeaseSchedulerWorkerMock).processPendingComputers();
		verifyNoMoreInteractions(endOfLeaseSchedulerWorkerMock);
	}
}
