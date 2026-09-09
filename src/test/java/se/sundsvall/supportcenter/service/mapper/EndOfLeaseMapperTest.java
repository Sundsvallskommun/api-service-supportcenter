package se.sundsvall.supportcenter.service.mapper;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportcenter.api.model.CreateEndOfLeaseBatchRequest;
import se.sundsvall.supportcenter.api.model.EndOfLeaseComputer;
import se.sundsvall.supportcenter.integration.db.model.EndOfLeaseComputerEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static se.sundsvall.supportcenter.integration.db.model.EndOfLeaseStatus.PENDING;
import static se.sundsvall.supportcenter.service.mapper.EndOfLeaseMapper.toEndOfLeaseBatchEntity;

class EndOfLeaseMapperTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String EXTERNAL_BATCH_ID = "d1f3a8c2-9b7e-4a5f-8c3d-2e6b1a4f7c90";
	private static final LocalDate END_OF_LEASE_DATE = LocalDate.of(2026, 11, 30);

	@Test
	void toEndOfLeaseBatchEntityMapsAllComputers() {
		final var createEndOfLeaseBatchRequest = CreateEndOfLeaseBatchRequest.create()
			.withExternalBatchId(EXTERNAL_BATCH_ID)
			.withComputers(List.of(
				EndOfLeaseComputer.create()
					.withSerialNumber("J123ABC")
					.withAssetTag("AB12345")
					.withEndOfLeaseDate(END_OF_LEASE_DATE),
				EndOfLeaseComputer.create()
					.withSerialNumber("K456DEF")
					.withAssetTag("PUB16604")
					.withEndOfLeaseDate(END_OF_LEASE_DATE)));

		final var endOfLeaseBatchEntity = toEndOfLeaseBatchEntity(MUNICIPALITY_ID, createEndOfLeaseBatchRequest);

		assertThat(endOfLeaseBatchEntity.getExternalBatchId()).isEqualTo(EXTERNAL_BATCH_ID);
		assertThat(endOfLeaseBatchEntity.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(endOfLeaseBatchEntity.getComputers())
			.extracting(EndOfLeaseComputerEntity::getSerialNumber, EndOfLeaseComputerEntity::getAssetTag, EndOfLeaseComputerEntity::getEndOfLeaseDate)
			.containsExactly(
				tuple("J123ABC", "AB12345", END_OF_LEASE_DATE),
				tuple("K456DEF", "PUB16604", END_OF_LEASE_DATE));
	}

	@Test
	void toEndOfLeaseBatchEntitySetsEveryComputerToPendingAndPointsItBackAtTheBatch() {
		final var createEndOfLeaseBatchRequest = CreateEndOfLeaseBatchRequest.create()
			.withExternalBatchId(EXTERNAL_BATCH_ID)
			.withComputers(List.of(EndOfLeaseComputer.create()
				.withSerialNumber("J123ABC")
				.withAssetTag("AB12345")
				.withEndOfLeaseDate(END_OF_LEASE_DATE)));

		final var endOfLeaseBatchEntity = toEndOfLeaseBatchEntity(MUNICIPALITY_ID, createEndOfLeaseBatchRequest);

		assertThat(endOfLeaseBatchEntity.getComputers()).hasSize(1).allSatisfy(computer -> {
			assertThat(computer.getSerialNumber()).isEqualTo("J123ABC");
			assertThat(computer.getAssetTag()).isEqualTo("AB12345");
			assertThat(computer.getEndOfLeaseDate()).isEqualTo(END_OF_LEASE_DATE);
			assertThat(computer.getStatus()).isEqualTo(PENDING);
			assertThat(computer.getBatch()).isSameAs(endOfLeaseBatchEntity);
			assertThat(computer.getAssetMunicipalityId()).isNull();
			assertThat(computer.getSentAt()).isNull();
		});
	}
}
