package se.sundsvall.supportcenter.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Address model")
public class Address {

	@NotBlank
	@Schema(example = "Street 1", required = true, description = "Street address")
	private String street;

	@NotBlank
	@Schema(example = "851 86", required = true, description = "Postal code")
	private String postalCode;

	@NotBlank
	@Schema(example = "Sundsvall", required = true, description = "City")
	private String city;

	public static Address create() {
		return new Address();
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public Address withStreet(String street) {
		this.street = street;
		return this;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public Address withPostalCode(String postalCode) {
		this.postalCode = postalCode;
		return this;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public Address withCity(String city) {
		this.city = city;
		return this;
	}


	@Override
	public int hashCode() {
		return Objects.hash(street, postalCode, city);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Address other = (Address) obj;
		return Objects.equals(street, other.street) && Objects.equals(postalCode, other.postalCode) && Objects.equals(city, other.city);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		return builder.append("Address [street=").append(street).append(", postalCode=").append(postalCode).append(", city=").append(city).append("]").toString();
	}
}
