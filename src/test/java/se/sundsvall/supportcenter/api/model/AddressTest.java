package se.sundsvall.supportcenter.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class AddressTest {

	@Test
	void isValidBean() {
		assertThat(Address.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var street = "street";
		final var postalCode = "postalCode";
		final var city = "city";
		final var address = Address.create()
			.withStreet(street)
			.withPostalCode(postalCode)
			.withCity(city);

		assertThat(address).hasNoNullFieldsOrProperties();
		assertThat(address.getStreet()).isEqualTo(street);
		assertThat(address.getPostalCode()).isEqualTo(postalCode);
		assertThat(address.getCity()).isEqualTo(city);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(Address.create()).hasAllNullFieldsOrProperties();
	}
}
