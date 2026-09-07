package se.sundsvall.supportcenter.service;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputer;

import static org.assertj.core.api.Assertions.assertThat;

class EndOfLeaseServiceTest {

	private final EndOfLeaseService endOfLeaseService = new EndOfLeaseService();

	@Test
	void registerBatchReturnsBatchId() {
		final var createEndOfLeaseBatchRequest = CreateEndOfLeaseBatchRequest.create()
			.withComputers(List.of(EndOfLeaseComputer.create()
				.withSerialNumber("J123ABC")
				.withAssetTag("WB16603")
				.withEndOfLeaseDate(LocalDate.of(2026, 11, 30))));

		final var batchId = endOfLeaseService.registerBatch("2281", createEndOfLeaseBatchRequest);

		assertThat(batchId).isNotBlank();
	}
}
