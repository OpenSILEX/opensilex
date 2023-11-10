package org.opensilex.faidare.model;

public class Faidarev1ExtendedContactDTO {
    private String name;
    private String type;
    private String email;
    private String instituteName;
    private String orcid;
    private String contactDbId;

    public String getName() {
        return name;
    }

    public Faidarev1ExtendedContactDTO setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public Faidarev1ExtendedContactDTO setType(String type) {
        this.type = type;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public Faidarev1ExtendedContactDTO setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getInstituteName() {
        return instituteName;
    }

    public Faidarev1ExtendedContactDTO setInstituteName(String instituteName) {
        this.instituteName = instituteName;
        return this;
    }

    public String getOrcid() {
        return orcid;
    }

    public Faidarev1ExtendedContactDTO setOrcid(String orcid) {
        this.orcid = orcid;
        return this;
    }

    public String getContactDbId() {
        return contactDbId;
    }

    public Faidarev1ExtendedContactDTO setContactDbId(String contactDbId) {
        this.contactDbId = contactDbId;
        return this;
    }
}
