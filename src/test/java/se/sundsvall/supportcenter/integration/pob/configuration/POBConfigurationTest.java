package se.sundsvall.supportcenter.integration.pob.configuration;

import feign.codec.Encoder;
import feign.jackson.JacksonEncoder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.JsonPathErrorDecoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportcenter.integration.pob.configuration.POBConfiguration.CLIENT_ID;

@ExtendWith(MockitoExtension.class)
class POBConfigurationTest {

	@Spy
	private FeignMultiCustomizer feignMultiCustomizerSpy;

	@Mock
	private FeignBuilderCustomizer feignBuilderCustomizerMock;

	@Mock
	private POBProperties propertiesMock;

	@Captor
	private ArgumentCaptor<Encoder> encoderCaptor;

	@Captor
	private ArgumentCaptor<JsonPathErrorDecoder> errorDecoderCaptor;

	@Test
	void testFeignBuilderCustomizer() {
		final var configuration = new POBConfiguration();
		final var connectTimeout = 123;
		final var readTimeout = 321;

		when(propertiesMock.connectTimeout()).thenReturn(connectTimeout);
		when(propertiesMock.readTimeout()).thenReturn(readTimeout);
		when(feignMultiCustomizerSpy.composeCustomizersToOne()).thenReturn(feignBuilderCustomizerMock);

		try (MockedStatic<FeignMultiCustomizer> feignMultiCustomizerMock = Mockito.mockStatic(FeignMultiCustomizer.class)) {
			feignMultiCustomizerMock.when(FeignMultiCustomizer::create).thenReturn(feignMultiCustomizerSpy);

			final var customizer = configuration.feignBuilderCustomizer(propertiesMock);

			feignMultiCustomizerMock.verify(FeignMultiCustomizer::create);

			verify(feignMultiCustomizerSpy).withEncoder(encoderCaptor.capture());
			verify(feignMultiCustomizerSpy).withErrorDecoder(errorDecoderCaptor.capture());
			verify(feignMultiCustomizerSpy).withRequestTimeoutsInSeconds(connectTimeout, readTimeout);
			verify(feignMultiCustomizerSpy).composeCustomizersToOne();

			assertThat(encoderCaptor.getValue()).isInstanceOf(JacksonEncoder.class);
			assertThat(errorDecoderCaptor.getValue())
				.isInstanceOf(JsonPathErrorDecoder.class)
				.hasFieldOrPropertyWithValue("bypassResponseCodes", List.of(NOT_FOUND.value()))
				.hasFieldOrPropertyWithValue("integrationName", CLIENT_ID);
			assertThat(customizer).isSameAs(feignBuilderCustomizerMock);
		}
	}
}
