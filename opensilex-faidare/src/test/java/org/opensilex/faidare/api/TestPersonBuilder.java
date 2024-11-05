package org.opensilex.faidare.api;

import org.opensilex.security.person.api.PersonDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class TestPersonBuilder {
    private static final String stringPrefix = "default person ";
    private URI uri = new URI("test:default-person-uri/");
    private String firstName = stringPrefix + "firstName ";
    private String lastName = stringPrefix + "lastName ";
    private String email = "default@email.org";
    private String affiliation = "default affiliation ";
    private String phoneNumber = "+33-0-00-00-00-0";
    private URI orcid;
    private URI account;

    public TestPersonBuilder() throws URISyntaxException {
    }

    public URI getUri() {
        return uri;
    }

    public TestPersonBuilder setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public TestPersonBuilder setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public TestPersonBuilder setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public TestPersonBuilder setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public TestPersonBuilder setAffiliation(String affiliation) {
        this.affiliation = affiliation;
        return this;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public TestPersonBuilder setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public URI getOrcid() {
        return orcid;
    }

    public TestPersonBuilder setOrcid(URI orcid) {
        this.orcid = orcid;
        return this;
    }

    public URI getAccount() {
        return account;
    }

    public TestPersonBuilder setAccount(URI account) {
        this.account = account;
        return this;
    }

    private final List<PersonDTO> dtoList = new ArrayList<>();

    public List<PersonDTO> getDTOList() {
        return dtoList;
    }

    public PersonDTO createDTO() throws URISyntaxException {
        PersonDTO dto = new PersonDTO();

        dto.setUri(new URI(getUri().toString() + dtoList.size()));
        dto.setFirstName(getFirstName() + dtoList.size());
        dto.setLastName(getLastName() + dtoList.size());
        dto.setEmail(getEmail() + dtoList.size());
        dto.setAffiliation(getAffiliation() + dtoList.size());
        dto.setPhoneNumber(getPhoneNumber() + dtoList.size());
        dto.setOrcid(getOrcid());
        dto.setAccount(getAccount());

        dtoList.add(dto);
        return dto;
    }
}
