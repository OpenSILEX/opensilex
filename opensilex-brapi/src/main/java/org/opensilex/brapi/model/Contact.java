//******************************************************************************
//                          Contact.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

/**
 * @see Brapi documentation V1.3 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3
 * @author Alice Boizet, Bernhard Gschloessl
 */
class Contact {
    private String contactDbId; //v2.1 : The ID which uniquely identifies this contact MIAPPE V1.1 (DM-33) Person ID
    private String email; //v2.1 : The contacts email address MIAPPE V1.1 (DM-32) Person email
    private String institutionName; //v2.1 : The name of the institution which this contact is part of MIAPPE V1.1 (DM-35) Person affiliation
    private String name; //v2.1 : The full name of this contact person MIAPPE V1.1 (DM-31) Person name
    private String orcid; //v2.1 : The Open Researcher and Contributor ID for this contact person (orcid.org) MIAPPE V1.1 (DM-33) Person ID
    private String type; //v2.1 : The type of person this contact represents (ex: Coordinator, Scientist, PI, etc.) MIAPPE V1.1 (DM-34) Person role

    public String getContactDbId() {
        return contactDbId;
    }

    public void setContactDbId(String contactDbId) {
        this.contactDbId = contactDbId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
