package org.opensilex.security.person.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@ApiModel
@JsonPropertyOrder({"orcid", "first_name", "last_name", "emails", "organizations"})
public class OrcidRecordDTO {
    @JsonProperty("orcid")
    protected URI orcid;

    @JsonProperty("first_name")
    protected String firstName;

    @JsonProperty("last_name")
    protected String lastName;

    @JsonProperty("emails")
    protected List<String> emails;

    /**
     * list of the organizations found in the employment part of the record
     */
    @JsonProperty("organizations")
    protected List<String> organizations;

    protected OrcidRecordDTO(JsonObject jsonRecord) {
        JsonObject personalInfo = jsonRecord.getJsonObject("person");

        JsonObject name = personalInfo.getJsonObject("name");
        JsonObject givenNamesObject = name.getJsonObject("given-names");
        firstName = givenNamesObject.getString("value");

        JsonObject familyName = name.getJsonObject("family-name");
        lastName = familyName.getString("value");

        emails = new ArrayList<>();
        JsonArray emailJsonArray = personalInfo.getJsonObject("emails").getJsonArray("email");
        List<JsonObject> emailList = emailJsonArray.getValuesAs(JsonObject.class);
        emailList.forEach( emailObject -> {
            emails.add(emailObject.getString("email"));
        });

        organizations = new ArrayList<>();
        JsonObject activities = jsonRecord.getJsonObject("activities-summary");
        JsonObject employments = activities.getJsonObject("employments");
        List<JsonObject> affiliations = employments.getJsonArray("affiliation-group").getValuesAs(JsonObject.class);
        affiliations.forEach( affiliationObject -> {
            JsonArray summaries = affiliationObject.getJsonArray("summaries");
            JsonObject employment = summaries.getJsonObject(0).getJsonObject("employment-summary");
            JsonObject organization = employment.getJsonObject("organization");
            organizations.add(organization.getString("name"));
        });

    }

    public URI getOrcid() {
        return orcid;
    }

    public void setOrcid(URI orcid) {
        this.orcid = orcid;
    }

    public String getFirstName() { return firstName; }

    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public List<String> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(List<String> organizations) {
        this.organizations = organizations;
    }
}
