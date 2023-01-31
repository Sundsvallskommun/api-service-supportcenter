package se.sundsvall.supportcenter.api.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidMacAddressConstraintValidatorTest {

	private ValidMacAddressConstraintValidator validator = new ValidMacAddressConstraintValidator();
	
	@ParameterizedTest
	@ValueSource(strings = {"00000ABB28FC", "00 00 0A BB 28 FC", "0000 0ABB 28FC", "00:00:0a:bb:28:fc", "0000:0ABB:28FC", "00.00.0A.BB.28.FC", "0000.0ABB.28FC"})
	@NullSource
	void testValidMacAddress(String macAddress) {
		assertThat(validator.isValid(macAddress, null)).isTrue();
	}
	
	@ParameterizedTest
	@ValueSource(strings = {"00:00:0a:bb:28::", "00000ABB28FCFC", "00000ABB28FH", "00000ABB28F", " ", "."})
	@EmptySource
	void testInvalidMacAddress(String invalidMacAddress) {
		assertThat(validator.isValid(invalidMacAddress, null)).isFalse();
	}
}
