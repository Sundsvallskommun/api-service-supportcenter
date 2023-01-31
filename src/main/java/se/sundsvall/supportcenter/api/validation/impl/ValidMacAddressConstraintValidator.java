package se.sundsvall.supportcenter.api.validation.impl;

import static java.util.Objects.isNull;
import static java.util.regex.Pattern.compile;

import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import se.sundsvall.supportcenter.api.validation.ValidMacAddress;

public class ValidMacAddressConstraintValidator implements ConstraintValidator<ValidMacAddress, String> {
	
	private static final String NON_HEXADECIMAL_VALUES = "[^a-fA-F0-9]";
	private static final String HEXADECIMAL_PAIRS = "^([0-9a-fA-F]{2}){6}$";
	
	private Pattern pattern = compile(HEXADECIMAL_PAIRS);
	   
	@Override
	public boolean isValid(final String macAddress, final ConstraintValidatorContext context) {
		if (isNull(macAddress)) return true;
		
		String normalized = macAddress.replaceAll(NON_HEXADECIMAL_VALUES, ""); // Remove all non hexadecimal values such as -, :, . from address
		return pattern.matcher(normalized).find(); // Match if there is 6 hexadecimal pairs in normalized string or not
	}
}
