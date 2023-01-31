package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Schema(description = "CreateCaseRequest model")
public class CreateCaseRequest {

	@Valid
	private Note note;

	@NotBlank
	@Schema(description = "Description", required = true, example = "Beställning av tjänst Test4")
	private String description;

	@NotBlank
	@Schema(description = "Case type", required = true, example = "Service Request")
	private String caseType;

	@NotBlank
	@Schema(description = "Case category", required = true, example = "IT Användarhantering")
	private String caseCategory;

	@NotBlank
	@Schema(description = "Customer contact", required = true, example = "test02test")
	private String customerContact;

	@NotBlank
	@Schema(description = "External article number", required = true, example = "4343434")
	private String externalArticleNumber;

	@NotBlank
	@Schema(description = "ManagementCompany", required = true, example = "BoU")
	private String managementCompany;

	@NotBlank
	@Schema(description = "Priority", required = true, example = "IT4")
	private String priority;

	@NotNull
	@Schema(description = "Personal", required = true, example = "true")
	private Boolean personal;

	@NotBlank
	@Schema(description = "Responsible group", required = true, example = "First Line IT")
	private String responsibleGroup;

	@NotBlank
	@Schema(description = "External service id", required = true, example = "123")
	private String externalServiceId;

	@NotNull
	@Schema(description = "Office", required = true, example = "true")
	private Boolean office;

	@NotBlank
	@Schema(description = "Free text", required = true, example = "leverans fredagar")
	private String freeText;

	@NotBlank
	@Schema(description = "Join contact", required = true, example = "test01test")
	private String joinContact;

	@NotBlank
	@Schema(description = "Responsibility number", required = true, example = "15220100")
	private String responsibilityNumber;

	@NotBlank
	@Schema(description = "Subaccount", required = true, example = "974310")
	private String subaccount;

	@NotBlank
	@Schema(description = "Business number", required = true, example = "920350")
	private String businessNumber;

	@NotBlank
	@Schema(description = "Activity number", required = true, example = "5662")
	private String activityNumber;

	@NotBlank
	@Schema(description = "Project number", required = true, example = "17071")
	private String projectNumber;

	@NotBlank
	@Schema(description = "Object number", required = true, example = "6310000")
	private String objectNumber;

	@NotBlank
	@Schema(description = "Counterpart", required = true, example = "115")
	private String counterPart;

	@NotBlank
	@Schema(description = "CI description", required = true, example = "Tydlig och bra CI beskrivning")
	private String ciDescription;

	@NotBlank
	@Schema(description = "Contact person", required = true, example = "Test Testsson")
	private String contactPerson;

	@NotBlank
	@Schema(description = "Phone number", required = true, example = "070-11111111")
	private String phoneNumber;

	@NotBlank
	@Schema(description = "Email", required = true, example = "test.testsson@sundsvall.se")
	private String email;

	@Valid
	private Address address;

	public static CreateCaseRequest create() {
		return new CreateCaseRequest();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public CreateCaseRequest withDescription(String description) {
		this.description = description;
		return this;
	}

	public String getCaseType() {
		return caseType;
	}

	public void setCaseType(String caseType) {
		this.caseType = caseType;
	}

	public CreateCaseRequest withCaseType(String caseType) {
		this.caseType = caseType;
		return this;
	}

	public String getCaseCategory() {
		return caseCategory;
	}

	public void setCaseCategory(String caseCategory) {
		this.caseCategory = caseCategory;
	}

	public CreateCaseRequest withCaseCategory(String caseCategory) {
		this.caseCategory = caseCategory;
		return this;
	}

	public String getCustomerContact() {
		return customerContact;
	}

	public void setCustomerContact(String customerContact) {
		this.customerContact = customerContact;
	}

	public CreateCaseRequest withCustomerContact(String customerContact) {
		this.customerContact = customerContact;
		return this;
	}

	public String getExternalArticleNumber() {
		return externalArticleNumber;
	}

	public void setExternalArticleNumber(String externalArticleNumber) {
		this.externalArticleNumber = externalArticleNumber;
	}

	public CreateCaseRequest withExternalArticleNumber(String externalArticleNumber) {
		this.externalArticleNumber = externalArticleNumber;
		return this;
	}

	public String getManagementCompany() {
		return managementCompany;
	}

	public void setManagementCompany(String managementCompany) {
		this.managementCompany = managementCompany;
	}

	public CreateCaseRequest withManagementCompany(String managementCompany) {
		this.managementCompany = managementCompany;
		return this;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public CreateCaseRequest withPriority(String priority) {
		this.priority = priority;
		return this;
	}

	public Boolean getPersonal() {
		return personal;
	}

	public void setPersonal(Boolean personal) {
		this.personal = personal;
	}

	public CreateCaseRequest withPersonal(Boolean personal) {
		this.personal = personal;
		return this;
	}

	public String getResponsibleGroup() {
		return responsibleGroup;
	}

	public void setResponsibleGroup(String responsibleGroup) {
		this.responsibleGroup = responsibleGroup;
	}

	public CreateCaseRequest withResponsibleGroup(String responsibleGroup) {
		this.responsibleGroup = responsibleGroup;
		return this;
	}

	public String getExternalServiceId() {
		return externalServiceId;
	}

	public void setExternalServiceId(String externalServiceId) {
		this.externalServiceId = externalServiceId;
	}

	public CreateCaseRequest withExternalServiceId(String externalServiceId) {
		this.externalServiceId = externalServiceId;
		return this;
	}

	public Boolean getOffice() {
		return office;
	}

	public void setOffice(Boolean office) {
		this.office = office;
	}

	public CreateCaseRequest withOffice(Boolean office) {
		this.office = office;
		return this;
	}

	public String getFreeText() {
		return freeText;
	}

	public void setFreeText(String freeText) {
		this.freeText = freeText;
	}

	public CreateCaseRequest withFreeText(String freeText) {
		this.freeText = freeText;
		return this;
	}

	public String getJoinContact() {
		return joinContact;
	}

	public void setJoinContact(String joinContact) {
		this.joinContact = joinContact;
	}

	public CreateCaseRequest withJoinContact(String joinContact) {
		this.joinContact = joinContact;
		return this;
	}

	public String getResponsibilityNumber() {
		return responsibilityNumber;
	}

	public void setResponsibilityNumber(String responsibilityNumber) {
		this.responsibilityNumber = responsibilityNumber;
	}

	public CreateCaseRequest withResponsibilityNumber(String responsibilityNumber) {
		this.responsibilityNumber = responsibilityNumber;
		return this;
	}

	public String getSubaccount() {
		return subaccount;
	}

	public void setSubaccount(String subaccount) {
		this.subaccount = subaccount;
	}

	public CreateCaseRequest withSubaccount(String subaccount) {
		this.subaccount = subaccount;
		return this;
	}

	public String getBusinessNumber() {
		return businessNumber;
	}

	public void setBusinessNumber(String businessNumber) {
		this.businessNumber = businessNumber;
	}

	public CreateCaseRequest withBusinessNumber(String businessNumber) {
		this.businessNumber = businessNumber;
		return this;
	}

	public String getActivityNumber() {
		return activityNumber;
	}

	public void setActivityNumber(String activityNumber) {
		this.activityNumber = activityNumber;
	}

	public CreateCaseRequest withActivityNumber(String activityNumber) {
		this.activityNumber = activityNumber;
		return this;
	}


	public String getProjectNumber() {
		return projectNumber;
	}

	public void setProjectNumber(String projectNumber) {
		this.projectNumber = projectNumber;
	}

	public CreateCaseRequest withProjectNumber(String projectNumber) {
		this.projectNumber = projectNumber;
		return this;
	}

	public String getObjectNumber() {
		return objectNumber;
	}

	public void setObjectNumber(String objectNumber) {
		this.objectNumber = objectNumber;
	}

	public CreateCaseRequest withObjectNumber(String objectNumber) {
		this.objectNumber = objectNumber;
		return this;
	}

	public String getCounterPart() {
		return counterPart;
	}

	public void setCounterPart(String counterPart) {
		this.counterPart = counterPart;
	}

	public CreateCaseRequest withCounterPart(String counterPart) {
		this.counterPart = counterPart;
		return this;
	}

	public String getCiDescription() {
		return ciDescription;
	}

	public void setCiDescription(String ciDescription) {
		this.ciDescription = ciDescription;
	}

	public CreateCaseRequest withCiDescription(String ciDescription) {
		this.ciDescription = ciDescription;
		return this;
	}

	public String getContactPerson() {
		return contactPerson;
	}

	public void setContactPerson(String contactPerson) {
		this.contactPerson = contactPerson;
	}

	public CreateCaseRequest withContactPerson(String contactPerson) {
		this.contactPerson = contactPerson;
		return this;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public CreateCaseRequest withPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
		return this;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public CreateCaseRequest withEmail(String email) {
		this.email = email;
		return this;
	}

	public Note getNote() {
		return note;
	}

	public void setNote(Note note) {
		this.note = note;
	}

	public CreateCaseRequest withNote(Note note) {
		this.note = note;
		return this;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public CreateCaseRequest withAddress(Address address) {
		this.address = address;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(note, description, caseType, caseCategory, customerContact, externalArticleNumber, managementCompany, priority, personal, responsibleGroup, externalServiceId, office, freeText,
				joinContact, responsibilityNumber, subaccount, businessNumber, activityNumber, projectNumber, objectNumber, counterPart, ciDescription, contactPerson, phoneNumber, email, address);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CreateCaseRequest other = (CreateCaseRequest) obj;
		return Objects.equals(note, other.note) && Objects.equals(description, other.description) && Objects.equals(caseType, other.caseType) && Objects.equals(caseCategory, other.caseCategory)
				&& Objects.equals(customerContact, other.customerContact) && Objects.equals(externalArticleNumber, other.externalArticleNumber) && Objects.equals(managementCompany, other.managementCompany)
				&& Objects.equals(priority, other.priority) && Objects.equals(personal, other.personal) && Objects.equals(responsibleGroup, other.responsibleGroup)
				&& Objects.equals(externalServiceId, other.externalServiceId) && Objects.equals(office, other.office) && Objects.equals(freeText, other.freeText)
				&& Objects.equals(joinContact, other.joinContact) && Objects.equals(responsibilityNumber, other.responsibilityNumber) && Objects.equals(subaccount, other.subaccount)
				&& Objects.equals(businessNumber, other.businessNumber) && Objects.equals(activityNumber, other.activityNumber) &&
				Objects.equals(projectNumber, other.projectNumber) && Objects.equals(objectNumber, other.objectNumber)
				&& Objects.equals(counterPart, other.counterPart) && Objects.equals(ciDescription, other.ciDescription) && Objects.equals(contactPerson, other.contactPerson)
				&& Objects.equals(phoneNumber, other.phoneNumber) && Objects.equals(email, other.email) && Objects.equals(address, other.address);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CreateCaseRequest [note=").append(note).append(", description=").append(description)
				.append(", caseType=").append(caseType).append(", caseCategory=").append(caseCategory).append(", customerContact=").append(customerContact)
				.append(", externalArticleNumber=").append(externalArticleNumber).append(", priority=").append(priority)
				.append(", managementCompany=").append(managementCompany)
				.append(", personal=").append(personal).append(", responsibleGroup=").append(responsibleGroup)
				.append(", externalServiceId=").append(externalServiceId).append(", office=").append(office)
				.append(", freeText=").append(freeText).append(", joinContact=").append(joinContact)
				.append(", responsibilityNumber=").append(responsibilityNumber).append(", subaccount=").append(subaccount)
				.append(", businessNumber=").append(businessNumber).append(", activityNumber=").append(activityNumber)
				.append(", projectNumber=").append(projectNumber).append(", objectNumber=").append(objectNumber).append(", counterPart=").append(counterPart)
				.append(", ciDescription=").append(ciDescription).append(", counterPart=").append(counterPart)
				.append(", contactPerson=").append(contactPerson).append(", phoneNumber=").append(phoneNumber)
				.append(", email=").append(email).append(", address=").append(address).append("]");
		return builder.toString();
	}
}