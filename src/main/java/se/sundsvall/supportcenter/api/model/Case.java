package se.sundsvall.supportcenter.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import java.util.Objects;

@Schema(description = "Case model")
public class Case {

    @Schema(description = "Case id", example = "81471222")
    private String caseId;

    @Valid
    private Note note;

    @Schema(description = "Description", example = "Beställning av tjänst Test4")
    private String description;

    @Schema(description = "Case type", example = "Service Request")
    private String caseType;

    @Schema(description = "Case category", example = "IT Användarhantering")
    private String caseCategory;

    @Schema(description = "Customer contact", example = "test02test")
    private String customerContact;

    @Schema(description = "External article number")
    private String externalArticleNumber;

    @Schema(description = "ManagementCompany", example = "BoU")
    private String managementCompany;

    @Schema(description = "Priority", example = "IT4")
    private String priority;

    @Schema(description = "Personal", example = "true")
    private Boolean personal;

    @Schema(description = "Responsible group", example = "First Line IT")
    private String responsibleGroup;

    @Schema(description = "External service id", example = "123")
    private String externalServiceId;

    @Schema(description = "Office", example = "true")
    private Boolean office;

    @Schema(description = "Free text", example = "leverans fredagar")
    private String freeText;

    @Schema(description = "Join contact", example = "test01test")
    private String joinContact;

    @Schema(description = "Responsibility number", example = "15220100")
    private String responsibilityNumber;

    @Schema(description = "Subaccount", example = "974310")
    private String subaccount;

    @Schema(description = "Business number", example = "920350")
    private String businessNumber;

    @Schema(description = "Activity number", example = "5662")
    private String activityNumber;

    @Schema(description = "Project number", example = "17071")
    private String projectNumber;

    @Schema(description = "Object number", example = "6310000")
    private String objectNumber;

    @Schema(description = "Counterpart", example = "115")
    private String counterPart;

    @Schema(description = "CI description", example = "Tydlig och bra CI beskrivning")
    private String ciDescription;

    @Schema(description = "Contact person", example = "Test Testsson")
    private String contactPerson;

    @Schema(description = "Phone number", example = "070-11111111")
    private String phoneNumber;

    @Schema(description = "Email", example = "test.testsson@sundsvall.se")
    private String email;

    @Valid
    private Address address;

    public static Case create() {
        return new Case();
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public Case withCaseId(String caseId) {
        this.caseId = caseId;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Case withDescription(String description) {
        this.description = description;
        return this;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public Case withCaseType(String caseType) {
        this.caseType = caseType;
        return this;
    }

    public String getCaseCategory() {
        return caseCategory;
    }

    public void setCaseCategory(String caseCategory) {
        this.caseCategory = caseCategory;
    }

    public Case withCaseCategory(String caseCategory) {
        this.caseCategory = caseCategory;
        return this;
    }

    public String getCustomerContact() {
        return customerContact;
    }

    public void setCustomerContact(String customerContact) {
        this.customerContact = customerContact;
    }

    public Case withCustomerContact(String customerContact) {
        this.customerContact = customerContact;
        return this;
    }

    public String getExternalArticleNumber() {
        return externalArticleNumber;
    }

    public void setExternalArticleNumber(String externalArticleNumber) {
        this.externalArticleNumber = externalArticleNumber;
    }

    public Case withExternalArticleNumber(String externalArticleNumber) {
        this.externalArticleNumber = externalArticleNumber;
        return this;
    }

    public String getManagementCompany() {
        return managementCompany;
    }

    public void setManagementCompany(String managementCompany) {
        this.managementCompany = managementCompany;
    }

    public Case withManagementCompany(String managementCompany) {
        this.managementCompany = managementCompany;
        return this;
    }


    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Case withPriority(String priority) {
        this.priority = priority;
        return this;
    }

    public Boolean getPersonal() {
        return personal;
    }

    public void setPersonal(Boolean personal) {
        this.personal = personal;
    }

    public Case withPersonal(Boolean personal) {
        this.personal = personal;
        return this;
    }

    public String getResponsibleGroup() {
        return responsibleGroup;
    }

    public void setResponsibleGroup(String responsibleGroup) {
        this.responsibleGroup = responsibleGroup;
    }

    public Case withResponsibleGroup(String responsibleGroup) {
        this.responsibleGroup = responsibleGroup;
        return this;
    }

    public String getExternalServiceId() {
        return externalServiceId;
    }

    public void setExternalServiceId(String externalServiceId) {
        this.externalServiceId = externalServiceId;
    }

    public Case withExternalServiceId(String externalServiceId) {
        this.externalServiceId = externalServiceId;
        return this;
    }

    public Boolean getOffice() {
        return office;
    }

    public void setOffice(Boolean office) {
        this.office = office;
    }

    public Case withOffice(Boolean office) {
        this.office = office;
        return this;
    }

    public String getFreeText() {
        return freeText;
    }

    public void setFreeText(String freeText) {
        this.freeText = freeText;
    }

    public Case withFreeText(String freeText) {
        this.freeText = freeText;
        return this;
    }

    public String getJoinContact() {
        return joinContact;
    }

    public void setJoinContact(String joinContact) {
        this.joinContact = joinContact;
    }

    public Case withJoinContact(String joinContact) {
        this.joinContact = joinContact;
        return this;
    }

    public String getResponsibilityNumber() {
        return responsibilityNumber;
    }

    public void setResponsibilityNumber(String responsibilityNumber) {
        this.responsibilityNumber = responsibilityNumber;
    }

    public Case withResponsibilityNumber(String responsibilityNumber) {
        this.responsibilityNumber = responsibilityNumber;
        return this;
    }

    public String getSubaccount() {
        return subaccount;
    }

    public void setSubaccount(String subaccount) {
        this.subaccount = subaccount;
    }

    public Case withSubaccount(String subaccount) {
        this.subaccount = subaccount;
        return this;
    }

    public String getBusinessNumber() {
        return businessNumber;
    }

    public void setBusinessNumber(String businessNumber) {
        this.businessNumber = businessNumber;
    }

    public Case withBusinessNumber(String businessNumber) {
        this.businessNumber = businessNumber;
        return this;
    }

    public String getActivityNumber() {
        return activityNumber;
    }

    public void setActivityNumber(String activityNumber) {
        this.activityNumber = activityNumber;
    }

    public Case withActivityNumber(String activityNumber) {
        this.activityNumber = activityNumber;
        return this;
    }

    public String getProjectNumber() {
        return projectNumber;
    }

    public void setProjectNumber(String projectNumber) {
        this.projectNumber = projectNumber;
    }

    public Case withProjectNumber(String projectNumber) {
        this.projectNumber = projectNumber;
        return this;
    }

    public String getObjectNumber() {
        return objectNumber;
    }

    public void setObjectNumber(String objectNumber) {
        this.objectNumber = objectNumber;
    }

    public Case withObjectNumber(String objectNumber) {
        this.objectNumber = objectNumber;
        return this;
    }

    public String getCounterPart() {
        return counterPart;
    }

    public void setCounterPart(String counterPart) {
        this.counterPart = counterPart;
    }

    public Case withCounterPart(String counterPart) {
        this.counterPart = counterPart;
        return this;
    }

    public String getCiDescription() {
        return ciDescription;
    }

    public void setCiDescription(String ciDescription) {
        this.ciDescription = ciDescription;
    }

    public Case withCiDescription(String ciDescription) {
        this.ciDescription = ciDescription;
        return this;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public Case withContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
        return this;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Case withPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Case withEmail(String email) {
        this.email = email;
        return this;
    }

    public Note getNote() {
        return note;
    }

    public void setNote(Note note) {
        this.note = note;
    }

    public Case withNote(Note note) {
        this.note = note;
        return this;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Case withAddress(Address address) {
        this.address = address;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, note, description, caseType, caseCategory, customerContact, externalArticleNumber, managementCompany, priority, personal, responsibleGroup, externalServiceId, office, freeText,
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
        Case other = (Case) obj;
        return Objects.equals(caseId, other.caseId) && Objects.equals(note, other.note) && Objects.equals(description, other.description) && Objects.equals(caseType, other.caseType) && Objects.equals(caseCategory, other.caseCategory)
                && Objects.equals(customerContact, other.customerContact) && Objects.equals(externalArticleNumber, other.externalArticleNumber)
                && Objects.equals(managementCompany, other.managementCompany) && Objects.equals(priority, other.priority) && Objects.equals(personal, other.personal) && Objects.equals(responsibleGroup, other.responsibleGroup)
                && Objects.equals(externalServiceId, other.externalServiceId) && Objects.equals(office, other.office) && Objects.equals(freeText, other.freeText)
                && Objects.equals(joinContact, other.joinContact) && Objects.equals(responsibilityNumber, other.responsibilityNumber) && Objects.equals(subaccount, other.subaccount)
                && Objects.equals(businessNumber, other.businessNumber) && Objects.equals(activityNumber, other.activityNumber) && Objects.equals(projectNumber, other.projectNumber)
                && Objects.equals(objectNumber, other.objectNumber) && Objects.equals(counterPart, other.counterPart) && Objects.equals(ciDescription, other.ciDescription)
                && Objects.equals(contactPerson, other.contactPerson) && Objects.equals(phoneNumber, other.phoneNumber) && Objects.equals(email, other.email)
                && Objects.equals(address, other.address);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Case [note=").append(note).append(", caseId=").append(caseId).append(", description=").append(description)
                .append(", caseType=").append(caseType).append(", caseCategory=").append(caseCategory).append(", customerContact=").append(customerContact)
                .append(", externalArticleNumber=").append(externalArticleNumber).append(", managementCompany=").append(managementCompany)
                .append(", priority=").append(priority).append(", personal=").append(personal).append(", responsibleGroup=").append(responsibleGroup)
                .append(", externalServiceId=").append(externalServiceId).append(", office=").append(office)
                .append(", freeText=").append(freeText).append(", joinContact=").append(joinContact)
                .append(", responsibilityNumber=").append(responsibilityNumber).append(", subaccount=").append(subaccount)
                .append(", businessNumber=").append(businessNumber).append(", projectNumber=").append(projectNumber)
                .append(", activityNumber=").append(activityNumber).append(", objectNumber=").append(objectNumber)
                .append(", ciDescription=").append(ciDescription).append(", counterPart=").append(counterPart)
                .append(", contactPerson=").append(contactPerson)
                .append(", phoneNumber=").append(phoneNumber).append(", email=").append(email).append(", address=").append(address).append("]");
        return builder.toString();
    }
}
