package se.sundsvall.supportcenter.service.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportcenter.integration.db.EndOfLeaseComputerRepository;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;

@ExtendWith(MockitoExtension.class)
class EndOfLeaseSchedulerWorkerTest {

	@Mock
	private EndOfLeaseComputerRepository endOfLeaseComputerRepositoryMock;

	@InjectMocks
	private EndOfLeaseSchedulerWorker endOfLeaseSchedulerWorker;

	@Test
	void processPendingComputers() {
		when(endOfLeaseComputerRepositoryMock.countByStatus(PENDING)).thenReturn(1000L);

		endOfLeaseSchedulerWorker.processPendingComputers();

		verify(endOfLeaseComputerRepositoryMock).countByStatus(PENDING);
		verifyNoMoreInteractions(endOfLeaseComputerRepositoryMock);
	}
}
